      subroutine coef8(p,vp1,vs1,ro1,vp2,vs2,ro2,ncode,nd,rmod,rph)
c
c     the routine coef8 is designed for the computation of reflection
c     and transmission coefficients at a plane interface between two
c     homogeneous solid halfspaces or at a free surface of a homogeneous
c     solid halfspace.
c
c     the codes of individual coefficients are specified by the
c     following numbers
c     a/ interface between two solid halfspaces
c     p1p1...1       p1s1...2       p1p2...3       p1s2...4
c     s1p1...5       s1s1...6       s1p2...7       s1s2...8
c     b/ free surface (for ro2.lt.0.00001)
c     pp.....1       px.....5       px,pz...x- and z- components of the
c     ps.....2       pz.....6       coef.of conversion,incident p wave
c     sp.....3       sx.....7       sx,sz...x- and z- components of the
c     ss.....4       sz.....8       coef.of conversion,incident s wave
c
c     i n p u t   p a r a m e t e r s
c           p...ray parameter
c           vp1,vs1,ro1...parameters of the first halfspace
c           vp2,vs2,ro2...parameters of second halfspace. for the free
c                    surface take ro2.lt.0.00001,eg.ro2=0., and
c                    arbitrary values of vp2 and vs2
c           ncode...code of the computed coefficient
c           nd...=0  when the positive direction of the ray
c                    and the x-axis make an acute angle
c                =1  when the wave impinges on the interface
c                    against the positive direction of the x-axis
c
c     o u t p u t   p a r a m e t e r s
c           rmod,rph...modul and argument of the coefficient
c
c     n o t e s
c     1/ positive p...in the direction of propagation
c     2/ positive s...to the left from p
c     3/ time factor of incident wave ... exp(-i*omega*t)
c     4/ formulae are taken from cerveny ,molotkov, psencik, ray method
c        in seismology, pages 30-35. due to the note 2, the signs at
c        certain coefficients are opposite
c
c       written by v.cerveny,1976
c       modified by t.j. owens, 3/22/82 see comments in code
c
      complex b(4),rr,c1,c2,c3,c4,h1,h2,h3,h4,h5,h6,h,hb,hc
      dimension prmt(4),d(4),dd(4)
c
      if(ro2.lt.0.000001)go to 150
      prmt(1)=vp1
      prmt(2)=vs1
      prmt(3)=vp2
      prmt(4)=vs2
      a1=vp1*vs1
      a2=vp2*vs2
      a3=vp1*ro1
      a4=vp2*ro2
      a5=vs1*ro1
      a6=vs2*ro2
      q=2.*(a6*vs2-a5*vs1)
      pp=p*p
      qp=q*pp
      x=ro2-qp
      y=ro1+qp
      z=ro2-ro1-qp

      g1=a1*a2*pp*z*z
      g2=a2*x*x
      g3=a1*y*y
      g4=a4*a5
      g5=a3*a6
      g6=q*q*pp
      do 21 i=1,4
      dd(i)=p*prmt(i)
   21 d(i)=sqrt(abs(1.-dd(i)*dd(i)))

      if(dd(1).le.1..and.dd(2).le.1..and.dd(3).le.1..and.dd(4).le.1.)
     1go to 100
c
c     complex coefficients
      do 22 i=1,4
      if(dd(i).gt.1.)go to 23
      b(i)=cmplx(d(i),0.)
      go to 22
   23 b(i)= cmplx(0.,d(i))
   22 continue
      c1=b(1)*b(2)
      c2=b(3)*b(4)
      c3=b(1)*b(4)
      c4=b(2)*b(3)
      h1=g1
      h2=g2*c1
      h3=g3*c2
      h4=g4*c3
      h5=g5*c4
      h6=g6*c1*c2
      h=1./(h1+h2+h3+h4+h5+h6)
      hb=2.*h
      hc=hb*p
      go to (1,2,3,4,5,6,7,8),ncode
    1 rr=h*(h2+h4+h6-h1-h3-h5)
      go to 26
    2 rr=vp1*b(1)*hc*(q*y*c2+a2*x*z)
      if(nd.ne.0)rr=-rr
      go to 26
    3 rr=a3*b(1)*hb*(vs2*b(2)*x+vs1*b(4)*y)
      go to 26
    4 rr=-a3*b(1)*hc*(q*c4-vs1*vp2*z)
      if(nd.ne.0)rr=-rr
      go to 26
    5 rr=-vs1*b(2)*hc*(q*y*c2+a2*x*z)
      if(nd.ne.0)rr=-rr
      go to 26
    6 rr=h*(h2+h5+h6-h1-h3-h4)
      go to 26
    7 rr=a5*b(2)*hc*(q*c3-vp1*vs2*z)
      if(nd.ne.0)rr=-rr
      go to 26
    8 rr=a5*b(2)*hb*(vp1*b(3)*y+vp2*b(1)*x)
      go to 26
c     real coefficients
  100 e1=d(1)*d(2)
      e2=d(3)*d(4)
      e3=d(1)*d(4)
      e4=d(2)*d(3)
      s1=g1
      s2=g2*e1
      s3=g3*e2
      s4=g4*e3
      s5=g5*e4
      s6=g6*e1*e2
      s=1./(s1+s2+s3+s4+s5+s6)
      sb=2.*s
      sc=sb*p
      go to (101,102,103,104,105,106,107,108),ncode
  101 r=s*(s2+s4+s6-s1-s3-s5)
      go to 250
  102 r=vp1*d(1)*sc*(q*y*e2+a2*x*z)
      if(nd.ne.0)r=-r
      go to 250
  103 r=a3*d(1)*sb*(vs2*d(2)*x+vs1*d(4)*y)
      go to 250
  104 r=-a3*d(1)*sc*(q*e4-vs1*vp2*z)
      if(nd.ne.0)r=-r
      go to 250
  105 r=-vs1*d(2)*sc*(q*y*e2+a2*x*z)
      if(nd.ne.0)r=-r
      go to 250
  106 r=s*(s2+s5+s6-s1-s3-s4)
      go to 250
  107 r=a5*d(2)*sc*(q*e3-vp1*vs2*z)
      if(nd.ne.0)r=-r
      go to 250
  108 r=a5*d(2)*sb*(vp1*d(3)*y+vp2*d(1)*x)
      go to 250
c
c     earths surface,complex coefficients and coefficients of conversion
c
c   n o t e :
c
c    signs of coefficients at loops 162, 166, & 168 have been changed
c    from the originnal version of coef8 due to inconsistencies in
c    notation from the cerveny, et al book
c    3/22/82
c
  150 a1=vs1*p
      a2=a1*a1
      a3=2.*a2
      a4=2.*a1
      a5=a4+a4
      a6=1.-a3
      a7=2.*a6
      a8=2.*a3*vs1/vp1
      a9=a6*a6
      dd(1)=p*vp1
      dd(2)=p*vs1
      do 151 i=1,2
  151 d(i)=sqrt(abs(1.-dd(i)*dd(i)))
      if(dd(1).le.1..and.dd(2).le.1.)go to 200
      do 154 i=1,2
      if(dd(i).gt.1.)go to 155
      b(i)=cmplx(d(i),0.)
      go to 154
  155 b(i)= cmplx(0.,d(i))
  154 continue
      h1=b(1)*b(2)
      h2=h1*a8
      h=1./(a9+h2)
      go to (161,162,163,164,165,166,167,168),ncode
  161 rr=(-a9+h2)*h
      go to 26
  162 rr=-a5*b(1)*h*a6
      if(nd.ne.0)rr=-rr
      go to 26
  163 rr=a5*b(2)*h*a6*vs1/vp1
      if(nd.ne.0)rr=-rr
      go to 26
  164 rr=-(a9-h2)*h
      go to 26
  165 rr=a5*h1*h
      if(nd.ne.0)rr=-rr
      go to 26
  166 rr=-a7*b(1)*h
      go to 26
  167 rr=a7*b(2)*h
      go to 26
  168 rr=a5*vs1*h1*h/vp1
      if(nd.ne.0)rr=-rr
   26 z2=real(rr)
      z3=aimag(rr)
      if(z2.eq.0..and.z3.eq.0.)go to 157
      rmod=sqrt(z2*z2+z3*z3)
      rph=atan2(z3,z2)
      return
  157 rmod=0.
      rph=0.
      return
c
c     earths surface,real coefficients and coefficients of conversion
c   n o t e :
c
c    signs of coeficients at loops 202, 206, & 208 have been reversed
c    by t.j. owens because of inconsistencies w/sign conventions
c    3/22/82
c
  200 s1=d(1)*d(2)
      s2=a8*s1
      s=1./(a9+s2)
      go to (201,202,203,204,205,206,207,208),ncode
  201 r=(-a9+s2)*s
      go to 250
  202 r=-a5*d(1)*s*a6
      if(nd.ne.0)r=-r
      go to 250
  203 r=a5*d(2)*s*a6*vs1/vp1
      if(nd.ne.0)r=-r
      go to 250
  204 r=(s2-a9)*s
      go to 250
  205 r=a5*s1*s
      if(nd.ne.0)r=-r
      go to 250
  206 r=-a7*d(1)*s
      go to 250
  207 r=a7*d(2)*s
      go to 250
  208 r=a5*vs1*s1*s/vp1
      if(nd.ne.0)r=-r
  250 if(r.lt.0.)go to 251
      rmod=r
      rph=0.
      return
  251 rmod=-r
      rph=-3.14159
      return
      end
