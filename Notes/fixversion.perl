#!/opt/local/bin/perl -i.bak

$datecmd = open(DATE, "date -u |");
$date = <DATE>;
close DATE;

$ver = "\@version 1.1.3 " . $date;

while (<>) {
	s/\@version.*/$ver/;
	print ;
}


