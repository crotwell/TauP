      subroutine coefsh(p,vs1,rho1,vs2,rho2,ncode,rmod,rph)
c
c  calculates sh-wave reflection and transmission coeficients
c    at a solid-solid interface of a free surface
c
c   solid-solid:  ncode =>
c
c    s1s1 = 1   s1s2 = 2
c
c   free surface: rho2 = 0.0 ncode =>
c
c    s1s1 = 1   free surface correction = 2
c
      complex p2,p4,d,h1,h2,rr
      if(rho2.lt..00001) go to 5
      a1=vs1*p
      a2=vs2*p
      b1=rho1*vs1
      b2=rho2*vs2
      g1=sqrt(abs(1. - a1*a1))
      g2=sqrt(abs(1. - a2*a2))
      p2=cmplx(g1,0.)
      p4=cmplx(g2,0.)
      if(a1.gt.1.) p2=cmplx(0.,g1)
      if(a2.gt.1.) p4=cmplx(0.,g2)
      h1=cmplx(b1,0.)*p2
      h2=cmplx(b2,0.)*p4
      d= h1 + h2
      go to (1,2) ncode
    1 rr=(h1-h2)/d
      go to 3
    2 rr=2.*h1/d
    3 z1=real(rr)
      z2=aimag(rr)
      if(z2.eq.0.) go to 4
      rmod=sqrt(z1*z1 + z2*z2)
      rph=atan2(z2,z1)
      return
    4 rmod=z1
      rph=0.
      return
c
c    free surface problem
c
    5 go to (6,7) ncode
    6 rmod=1.
      rph=0.
      return
    7 rmod=2.
      rph=0.
      return
      end
