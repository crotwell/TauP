      integer taupinit, taupcalculate, taupsetdepth, taupgetnumarrivals
      integer taupappedphases, taupclearphases
      character mod*8, taupgetarrivalname*16

      h = 100.0
      mod = 'prem'

      init = taupinit(mod)

      if (init .eq. 0) stop '**Unable to initialize'

      if (taupappendphases(init, 'P,S,PcP,ScS') .ne. 0) then
         stop '**Unable to define phases'
      endif

      if (taupsetdepth(init, h) .ne. 0) then
         stop '**Unable to set depth'
      endif

      if (taupcalculate(init, 60.0) .ne. 0) then
         stop '**Unable to calculate times'
      endif

      call report(mod,init)

      if (taupclearphases(init) .ne. 0) then
         stop '**Unable to clear phases'
      endif

      if (taupappendphases(init,'PKP,SKP,PKIKP,SKIKS,PKKP,SKKS')
     &   .ne.0) then
         stop '**Unable to redefine phases'
      endif

      if (taupsetdepth(init, h) .ne. 0) then
         stop '**Unable to set depth'
      endif

      if (taupcalculate(init, 160.0) .ne. 0) then
         stop '**Unable to calculate times'
      endif

      call report(mod,init)

      if (taupdestroy(init) .ne. 0) then
         stop '**Unable to destroy calculator'
      endif
      end

      subroutine report(mod, init)
      integer taupgetnumarrivals
      character mod*(*), arr*16, taupgetarrivalname*16

      if (taupgetnumarrivals(init) .gt. 0) then
1000     format(a,' arrivals at ',f8.2,' deg. for ',f6.1,' km source')
1001     format(a8,1x,f8.2,f10.4)
         h = taupgetarrivalsourcedepth(init, 1)
         deg = taupgetarrivaldist(init, 1)
         write(*,1000) mod(1:nblen(mod)),deg,h
	 do i=1,taupgetnumarrivals(init)
	    arr = taupgetarrivalname(init, i)
	    tt = taupgetarrivaltime(init, i)
	    p = taupgetarrivalrayparam(init, i)
            write(*,1001) arr,tt,p
	 enddo
      else
         write(*,*) '**No arrivals at ',deg,' degrees.'
      endif
      end

      function nblen(str)
      character str*(*)

      do i=len(str),1,-1
	 if (str(i:i) .ne. ' ') go to 10
      enddo
      i = 1
10    continue
      nblen = i
      end
