#!/usr/bin/perl
#*******************************************************************************
#* Copyright (c) 2012 IBM Corporation and others.
#* All rights reserved. This program and the accompanying materials
#* are made available under the terms of the Eclipse Public License v1.0
#* which accompanies this distribution, and is available at
#* http://www.eclipse.org/legal/epl-v10.html
#*******************************************************************************/

use strict;

my $patint  = "([\\+\\-\\d]+)";              # Pattern for Integer number
my $patnode = "([\^\\s]+(\\.[\^\\s]*)*)";    # Pattern for domain name (a.b.c)
my $patbl = "\\s*";    # Pattern for blank space (variable length)

my $portbase     = 50000;
my $portrange    = 10000;
my $verbose      = 0;
my $ROUTING_FILE = "routing_file";
my $TOTAL_PROCS  = 0;
my @JOB;

my $pid;
my $hpcrun;
my $launchMode;
my $debuggerId;
my @child_pids;

# Waits for the attach.cfg file to appear, then checks that the whole
# file has been written. Returns when the file is available.
sub wait_for_cfg_file {
	my ($file) = @_;
	my $line;
	my $ntasks = -1;
	my $nlines = 0;

	while (1) {
		while ( !open( CFG, "<", $file ) ) {
			sleep(1);
		}

		# Ignore first line
		$line = <CFG>;

		# Second line has number of tasks
		chomp( $line = <CFG> );
		if ( $line =~ /$patint$patbl;/ ) {
			$ntasks = $1;
		}
		# Count the number of lines in the rest of the file
		$nlines = 0;
		while ( $line = <CFG> ) {
			$nlines++;
		}
		close(CFG);
		if ($ntasks == $nlines) {
			return;
		}
		sleep(1);
	}
}

#####################################################################
#
# The routing table is called 'routing_file' and it is generated in
# the current working directory. The sdm's working directory must be
# the same location if they are to find the table. Also, any old
# routing tables should be removed before starting the sdm.
#
# Routing table format is:
#
# num_tasks
# task_num host_name port_num
# ...
#
# where:
# 	num_tasks is the total number of tasks in the MPI job
#	task_num is the task number for a process (e.g. 0, 1, 2, etc.)
#	host_name is the hostname of the node the process is running on
#	port_num is a semi-random port number that the debugger will listen on
#
#####################################################################
sub generate_routing_table {
	my ($pid) = @_;
	my $line;
	my $ntasks;
	my $fd;
	my $cfg_file = "/tmp/.ppe.$pid.attach.cfg";

	wait_for_cfg_file($cfg_file);
	
	if ( open(CFG, $cfg_file) ) {

		open( OUT, "> $ROUTING_FILE" ) || die "cannot open file $ROUTING_FILE";

		# Ignore first line
		$line = <CFG>;

		# Second line has number of tasks
		chomp( $line = <CFG> );
		if ( $line =~ /$patint$patbl;/ ) {
			$ntasks = $1;
			printf(OUT "%d\n", $ntasks);
		}
		while ( $line = <CFG> ) {
			my ( $taskid, $nodeid );
			chomp($line);
			if ( $line =~ /$patint$patbl$patint$patbl$patnode/ ) {
				( $taskid, $nodeid ) = ( $1, $3 );
			} elsif ( $line =~ /$patint$patbl$patnode/ ) {
				( $taskid, $nodeid ) = ( $1, $2 );
			}
			printf( OUT "%d %s %d\n",
				$taskid, $nodeid, $portbase + int( rand($portrange) ) );
		}

		close(CFG);
		close(OUT);
	}
}

sub start_sdm_master {
	my($cmd, @args) = @_;
	my $pid;
	
	$pid = fork();
	if ($pid == 0) {
		exec($cmd, '--master', @args);
		exit(1);
	}
	push(@child_pids, $pid);
}

$launchMode = $ENV{'PTP_LAUNCH_MODE'};
$debuggerId = $ENV{'PTP_DEBUGGER_ID'};
$hpcrun = $ENV{'HPC_USE_HPCRUN'};

if ($launchMode eq 'debug' && $debuggerId eq 'org.eclipse.ptp.debug.sdm') {
	unlink($ROUTING_FILE);		
}

$pid = fork();
if ( $pid == 0 ) {
	printf( "::::PoePid=%d::::\n", $$ );
    if ($hpcrun eq 'yes') {
        exec('/opt/ibmhpc/ppedev.hpct/bin/hpcrun', '/usr/bin/poe', @ARGV);
    }
    else {
        exec('poe', @ARGV);
    }
	exit(1);
}
push(@child_pids, $pid);

if ($launchMode eq 'debug' && $debuggerId eq 'org.eclipse.ptp.debug.sdm') {
	generate_routing_table($pid);
	start_sdm_master(@ARGV);
}

foreach (@child_pids) {
	waitpid($_, 0);
}

exit( $? >> 8 );
