#!/bin/csh -f

setenv ANT_HOME /usr/local/apache-ant-1.9.4

$ANT_HOME/bin/ant $1 $2 $3 $4 $5 $6 $7 $8 $9

unsetenv ANT_HOME

exit
