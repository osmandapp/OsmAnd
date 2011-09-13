#! /usr/bin/ksh

#  datecalc -- Perderabo's date calculator   
#

USAGE="\
datecalc -a year month day - year month day
datecalc -a year month day [-|+] n
datecalc -d year month day
datecalc -D year month day
datecalc -j year month day
datecalc -j n
datecalc -l year month
use \"datecalc -help\" use for more documentation"

DOCUMENTATION="\
  datecalc  Version 1.1

  datecalc does many manipulations with dates.
  datecalc -a is for date arithmetic
  datecalc -d or -D converts a date to the day of week
  datecalc -j converts to date to or from julian day
  datecalc -l outputs the last day of a month

  All dates must be between the years 1860 and 3999.

  datecalc -a followed by 7 parameters will calculate the
  number of days between two dates.  Parameters 2-4 and 6-8
  must be dates in ymd form, and parameter 5 must be a minus
  sign.  The output is an integer.  Example:

  > datecalc -a 1960 12 31 - 1922 2 2
  14212


  datecalc -a followed by 5 parameters will calculate the
  a new date offset from a given date,  Parameters 2-4 must
  be a date in ymd form, paramter 5 must be + or -, and 
  paramter 6 must be an integer.  Output is a new date.
  Example:

  > datecalc -a 1960 12 31 + 7
  1961 1 7


  datecalc -d followed by 3 parameters will convert a date
  to a day-of-week.  Parameters 2-4 must be a date in ymd 
  form.  Example:

  > datecalc -d 1960 12 31
  6


  datecalc -D is like -d except it displays the name of
  the day.  Example:

  > datecalc -D 1960 12 31
  Saturday


  datecalc -j followed by 3 parameters will convert a date
  to Modified Julian Day number.  Example:
  > datecalc -j 1960 12 31
  37299


  datecalc -j followed by a single parameter will convert
  a Modified Julian Day number to a date.  Example:
  > datecalc -j 37299
  1960 12 31


  datecalc -l followed by year and month will output the last
  day of that month.  Note that by checking the last day of
  February you can test for leap year.  Example:
  > datecalc -l 2002 2
  28"


lastday()  {
        integer year month leap
#                         ja fe ma ap ma jn jl ag se oc no de
        set -A mlength xx 31 28 31 30 31 30 31 31 30 31 30 31

        year=$1
        if ((year<1860 || year> 3999)) ; then
                print -u2 year out of range
                return 1
        fi
        month=$2
        if ((month<1 || month> 12)) ; then
                print -u2 month out of range
                return 1
        fi

        if ((month != 2)) ; then
                print ${mlength[month]}
                return 0
        fi

        leap=0
        if ((!(year%100))); then
                ((!(year%400))) && leap=1
        else
                ((!(year%4))) && leap=1
        fi

        feblength=28
        ((leap)) && feblength=29
        print $feblength
        return 0
}


date2jd() {
        integer ijd day month year mnjd jd lday

        year=$1
        month=$2
        day=$3
        lday=$(lastday $year $month) || exit $?

        if ((day<1 || day> lday)) ; then
                print -u2 day out of range
                return 1
        fi

        ((standard_jd = day - 32075 
           + 1461 * (year + 4800 - (14 - month)/12)/4 
           + 367 * (month - 2 + (14 - month)/12*12)/12 
           - 3 * ((year + 4900 - (14 - month)/12)/100)/4))
        ((jd = standard_jd-2400001))


        print $jd
        return 0
}


jd2dow()
{
        integer jd dow numeric_mode
        set +A days Sunday Monday Tuesday Wednesday Thursday Friday Saturday

        numeric_mode=0
        if [[ $1 = -n ]] ; then
                numeric_mode=1
                shift
        fi


        jd=$1
        if ((jd<1 || jd>782028)) ; then
                print -u2 julian day out of range
                return 1
        fi

        ((dow=(jd+3)%7))

        if ((numeric_mode)) ; then
                print $dow
        else
                print ${days[dow]}
        fi
        return
}

jd2date()
{
        integer standard_jd temp1 temp2 jd year month day

        jd=$1
        if ((jd<1 || jd>782028)) ; then
                print julian day out of range
                return 1
        fi
        ((standard_jd=jd+2400001))
        ((temp1 = standard_jd + 68569))
        ((temp2 = 4*temp1/146097))
        ((temp1 = temp1 - (146097 * temp2 + 3) / 4))
        ((year  = 4000 * (temp1 + 1) / 1461001))
        ((temp1 = temp1 - 1461 * year/4 + 31))
        ((month = 80 * temp1 / 2447))
        ((day   = temp1 - 2447 * month / 80))
        ((temp1 = month / 11))
        ((month = month + 2 - 12 * temp1))
        ((year  = 100 * (temp2 - 49) + year + temp1))
        print $year $month $day
        return 0
}


#
#  Parse parameters and get to work.
case $1 in
-a)     if (($# == 8)) ; then
                if [[ $5 != - ]] ; then
                        print -u2 - "$USAGE"
                        exit 1
                fi
                jd1=$(date2jd $2 $3 $4) || exit $?
                jd2=$(date2jd $6 $7 $8) || exit $?
                ((jd3=jd1-jd2))
                print $jd3
                exit 0
        elif (($# == 6)) ; then
                jd1=$(date2jd $2 $3 $4) || exit $?
                case $5 in 
                -|+) eval '(('jd2=${jd1}${5}${6}'))'
                        jd2date $jd2
                        exit $?
                        ;;
                *)
                        print -u2 - "$USAGE"
                        exit 1
                        ;;
                esac
                        
        fi
        ;;

-d|-D)  if (($# != 4)) ; then
                print -u2 - "$USAGE"
                exit 1
        fi
        jd1=$(date2jd $2 $3 $4) || exit $?
        numeric=-n
        [[ $1 = -D ]] && numeric=""
        eval jd2dow $numeric $jd1 
        exit $?
        ;;

-j)     if (($# == 4)) ; then
                date2jd $2 $3 $4
                exit $?
        elif (($# == 2)) ; then
                jd2date $2 $3 $4
                exit $?
        else
                print -u2 - "$USAGE"
                exit 1
        fi
        ;;

-l)      if (($# == 3)) ; then
                lastday $2 $3
                exit $?
        else
                print -u2 - "$USAGE"
                exit 1
        fi
        ;;

-help)  print - "$USAGE"
        print  ""
        print - "$DOCUMENTATION"
        exit 0
        ;;

*)      print -u2 - "$USAGE"
        exit 0
        ;;


esac

#not reached
exit 7
