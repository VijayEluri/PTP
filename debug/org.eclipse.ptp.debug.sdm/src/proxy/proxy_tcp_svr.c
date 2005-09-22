/******************************************************************************
 * Copyright (c) 2005 The Regents of the University of California. 
 * This material was produced under U.S. Government contract W-7405-ENG-36 
 * for Los Alamos National Laboratory, which is operated by the University 
 * of California for the U.S. Department of Energy. The U.S. Government has 
 * rights to use, reproduce, and distribute this software. NEITHER THE 
 * GOVERNMENT NOR THE UNIVERSITY MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR 
 * ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE. If software is modified 
 * to produce derivative works, such modified software should be clearly 
 * marked, so as not to confuse it with the version available from LANL.
 * 
 * Additionally, this program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * LA-CC 04-115
 ******************************************************************************/

/*
 * The proxy handles communication between the client debug library API and the
 * client debugger, since they may be running on different hosts, and will
 * certainly be running in different processes.
 */

#include <sys/socket.h>
#include <arpa/inet.h>
#include <netdb.h>

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>

#include "compat.h"
#include "dbg.h"
#include "dbg_client.h"
#include "args.h"
#include "session.h"
#include "proxy.h"
#include "proxy_tcp.h"
#include "procset.h"

static int	proxy_tcp_svr_create(void **, void (*)(void));
static int	proxy_tcp_svr_progress(void *);
static void	proxy_tcp_svr_finish(void *);

static int	proxy_tcp_svr_dispatch(int, void *);
static int	proxy_tcp_svr_accept(int, void *);

proxy_svr_funcs proxy_tcp_svr_funcs =
{
	proxy_tcp_svr_create,
	proxy_tcp_svr_progress,
	proxy_tcp_svr_dispatch,
	proxy_tcp_svr_finish,
};

struct proxy_tcp_svr_func {
	char *cmd;
	int (*func)(char **, char **);
};

typedef struct proxy_tcp_svr_func	proxy_tcp_svr_func;

static int proxy_tcp_svr_setlinebreakpoint(char **, char **);
static int proxy_tcp_svr_setfuncbreakpoint(char **, char **);
static int proxy_tcp_svr_deletebreakpoints(char **, char **);
static int proxy_tcp_svr_go(char **, char **);
static int proxy_tcp_svr_step(char **, char **);
static int proxy_tcp_svr_liststackframes(char **, char **);
static int proxy_tcp_svr_setcurrentstackframe(char **, char **);
static int proxy_tcp_svr_evaluateexpression(char **, char **);
static int proxy_tcp_svr_listlocalvariables(char **, char **);
static int proxy_tcp_svr_listarguments(char **, char **);
static int proxy_tcp_svr_listglobalvariables(char **, char **);
static int proxy_tcp_svr_quit(char **, char **);

static proxy_tcp_svr_func proxy_tcp_svr_func_tab[] =
{
	{"SLB",	proxy_tcp_svr_setlinebreakpoint},
	{"SFB",	proxy_tcp_svr_setfuncbreakpoint},
	{"DBS",	proxy_tcp_svr_deletebreakpoints},
	{"GOP",	proxy_tcp_svr_go},
	{"STP",	proxy_tcp_svr_step},
	{"LSF",	proxy_tcp_svr_liststackframes},
	{"SCS",	proxy_tcp_svr_setcurrentstackframe},
	{"EEX",	proxy_tcp_svr_evaluateexpression},
	{"LLV",	proxy_tcp_svr_listlocalvariables},
	{"LAR",	proxy_tcp_svr_listarguments},
	{"LGV",	proxy_tcp_svr_listglobalvariables},
	{"QUI",	proxy_tcp_svr_quit},
	{NULL,	NULL},
};

static int proxy_tcp_svr_shutdown;
static void (*proxy_tcp_svr_shutdown_callback)(void);

/*
 * Called when an event is received in response to a client debug command.
 * Sends the event to the proxy peer.
 */
static void
proxy_tcp_svr_event_callback(dbg_event *ev, void *data)
{
	proxy_tcp_conn *	conn = (proxy_tcp_conn *)data;
	char *			str;
	
	(void)proxy_tcp_event_to_str(ev, &str);
	(void)proxy_tcp_send_msg(conn, str, strlen(str));
	free(str);
}

/**
 * Create server socket and bind address to it. 
 * 
 * @return conn structure containing server socket and port
 */
static int 
proxy_tcp_svr_create(void **data, void (*shutdown)(void))
{
	socklen_t			slen;
	SOCKET				sd;
	struct sockaddr_in	sname;
	proxy_tcp_conn		*conn;
	
	if ( (sd = socket(PF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET )
	{
		fprintf(stderr, "socket error");
		return -1;
	}
	
	memset (&sname, 0, sizeof(sname));
	sname.sin_family = PF_INET;
	sname.sin_port = htons(PROXY_TCP_PORT);
	sname.sin_addr.s_addr = htonl(INADDR_ANY);
	
	if (bind(sd,(struct sockaddr *) &sname, sizeof(sname)) == SOCKET_ERROR )
	{
		fprintf(stderr, "bind error\n");
		CLOSE_SOCKET(sd);
		return -1;
	}
	
	slen = sizeof(sname);
	
	if ( getsockname(sd, (struct sockaddr *)&sname, &slen) == SOCKET_ERROR )
	{
		fprintf(stderr, "getsockname error\n");
		CLOSE_SOCKET(sd);
		return -1;
	}
	
	if ( listen(sd, 5) == SOCKET_ERROR )
	{
		fprintf(stderr, "listen error\n");
		CLOSE_SOCKET(sd);
		return -1;
	}
	
	proxy_tcp_create_conn(&conn);
	
	conn->svr_sock = sd;
	conn->port = (int) ntohs(sname.sin_port);
	*data = (void *)conn;
	
	DbgClntRegisterFileHandler(sd, READ_FILE_HANDLER, proxy_tcp_svr_accept, (void *)conn);
	DbgClntRegisterEventHandler(proxy_tcp_svr_event_callback, (void *)conn);
	
	proxy_tcp_svr_shutdown = 0;
	proxy_tcp_svr_shutdown_callback = shutdown;
	
	return 0;
}

/**
 * Accept a new proxy connection. Register dispatch routine.
 */
static int
proxy_tcp_svr_accept(int fd, void *data)
{
	proxy_tcp_conn *	conn = (proxy_tcp_conn *)data;
	socklen_t		fromlen;
	SOCKET			ns;
	struct sockaddr	addr;
	
	fromlen = sizeof(addr);
	ns = accept(fd, &addr, &fromlen);
	if (ns < 0) {
		perror("accept");
		return 0;
	}
	
	/*
	 * Only allow one connection at a time.
	 */
	if (conn->sock != INVALID_SOCKET) {
		CLOSE_SOCKET(ns); // reject
		return 0;
	}
	
	conn->sock = ns;
	
	DbgClntRegisterFileHandler(ns, READ_FILE_HANDLER, proxy_tcp_svr_dispatch, (void *)conn);
	
	return 0;
	
}

/**
 * Cleanup prior to server exit.
 */
static void 
proxy_tcp_svr_finish(void *data)
{
	proxy_tcp_conn *	conn = (proxy_tcp_conn *)data;
	proxy_tcp_destroy_conn(conn);
}

/**
 * Check for incoming messages or connection attempts.
 */
static int
proxy_tcp_svr_progress(void *data)
{
	proxy_tcp_conn *	conn = (proxy_tcp_conn *)data;

	if (proxy_tcp_svr_shutdown) {
		if (conn->sock != INVALID_SOCKET) {
			DbgClntUnregisterFileHandler(conn->sock);
			CLOSE_SOCKET(conn->sock);
			conn->sock = INVALID_SOCKET;
		}
		if (conn->svr_sock != INVALID_SOCKET) {
			DbgClntUnregisterFileHandler(conn->svr_sock);
			CLOSE_SOCKET(conn->svr_sock);
			conn->svr_sock = INVALID_SOCKET;
		}
		proxy_tcp_svr_shutdown_callback();
		return 0;
	}
	
	return DbgClntProgress();
}

/*
 * Dispatch a command to the server
 *
 * proxy_tcp_svr_dispatch() should never fail. If we get a read error from the client then we just
 * assume the client has gone away. Errors from server commands are just reported back to the
 * client.
 */
static int
proxy_tcp_svr_dispatch(int fd, void *data)
{
	int					i;
	int					n;
	int					res;
	char *				msg;
	char **				args;
	char *				response;
	proxy_tcp_svr_func * sf;
	proxy_tcp_conn *		conn = (proxy_tcp_conn *)data;
	
	n = proxy_tcp_recv_msg(conn, &msg);
	if (n <= 0)
		return n;
	
	args = Str2Args(msg);
	
	response = NULL;

	for (i = 0; i < sizeof(proxy_tcp_svr_func_tab) / sizeof(proxy_tcp_svr_func); i++) {
		sf = &proxy_tcp_svr_func_tab[i];
		if (sf->cmd != NULL && strcmp(args[0], sf->cmd) == 0) {
			res = sf->func(args, NULL);
			break;
		}
	}
	
	FreeArgs(args);
	free(msg);
	
	if (res != DBGRES_OK)
		asprintf(&response, "%d %d \"%s\"", res, DbgGetError(), DbgGetErrorStr());
	else
		asprintf(&response, "%d", res);
			

	
	free(response);
	
	return 0;
}

/*
 * SERVER FUNCTIONS
 */
static int 
proxy_tcp_svr_setlinebreakpoint(char **args, char **response)
{
	int			res;
	char *		file;
	int			line;
	procset *	procs;
	
	file = args[2];
	line = atoi(args[3]);
	
	procs = str_to_procset(args[1]);
	if (procs == NULL) {
		DbgSetError(DBGERR_PROCSET, NULL);
		return DBGRES_ERR;
	}
	
	res = DbgClntSetLineBreakpoint(procs, file, line);
	
	procset_free(procs);
	
	return res;
}

static int 
proxy_tcp_svr_setfuncbreakpoint(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_deletebreakpoints(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_go(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_step(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_liststackframes(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_setcurrentstackframe(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_evaluateexpression(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_listlocalvariables(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_listarguments(char **args, char **response)
{
	return DBGRES_OK;
}

static int 
proxy_tcp_svr_listglobalvariables(char **args, char **response)
{
	return DBGRES_OK;
}

static int
proxy_tcp_svr_quit(char **args, char **response)
{
	int	res;
	
	res = DbgClntQuit();
	
	proxy_tcp_svr_shutdown++;
	
	return res;
}