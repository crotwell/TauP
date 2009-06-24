#!/bin/bash

OUT=ak135
rm -f ${OUT}.ps

DELTA=/tmp/ak135.deltaTau

psxy -P -B10/.01 -JX6/6 -R0/100/-0.093/0.095 -St.003i -G255 -K > ${OUT}.ps <<END
END
groovy -na -e 'println(split[0]+" "+(split[2].toFloat()-split[1].toFloat()))' $DELTA | psxy -JX -R -Wdefault/red -K -O >> ${OUT}.ps

groovy -na -e 'println(split[0]+" "+(split[3].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/green -O -K -Y1 >> ${OUT}.ps
groovy -na -e 'println(split[0]+" "+(split[4].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/yellow -O -K  >> ${OUT}.ps
groovy -na -e 'println(split[0]+" "+(split[5].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/black -O -K  >> ${OUT}.ps
groovy -na -e 'println(split[0]+" "+(split[6].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/cyan -O -K  >> ${OUT}.ps
groovy -na -e 'println(split[0]+" "+(split[7].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/black -O -K  >> ${OUT}.ps
groovy -na -e 'println(split[0]+" "+(split[7].toFloat()-split[2].toFloat()))' $DELTA | psxy -JX -R -P -Wdefault/blue -O -K >> ${OUT}.ps

psxy -JM1 -R0/1/0/1 -St.003i -G255 -O >> ${OUT}.ps <<END
END

pstopdf ${OUT}.ps
open ${OUT}.pdf
