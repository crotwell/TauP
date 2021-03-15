
@Grab('edu.sc.seis:TauP:2.5.0')
import edu.sc.seis.TauP.TauP_Time
import edu.sc.seis.TauP.Arrival
import edu.sc.seis.TauP.TimeDist

def taup_time = new TauP_Time("prem")

taup_time.clearPhaseNames()
taup_time.parsePhaseList("P,PcP,S,ScS")
taup_time.setSourceDepth(100)
taup_time.calculate(60)
for(Arrival a: taup_time.getArrivals()) {
  println a.name+"  "+a.time
  for(TimeDist piercePoint : a.getPierce()) {
    println "  "+piercePoint.distDeg +" "+piercePoint.time
  }
}
