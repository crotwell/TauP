.. _amplitude_formula:

Amplitude Formula
^^^^^^^^^^^^^^^^^^^^^

The amplitude calculations follow Chapters 13 and 17 of :cite:t:`fmgs`.
Equation numbers referenced below correspond to those in :cite:t:`fmgs`.
The displacement amplitude of a seismic phase is computed as:

.. math::

    \boxed{
    u(\mathbf{x,t})
    \sim
    \left(\frac{\dot{M}(t) F} {R_s}\right)  \times
    G(\Delta,h)\times
    \left[\prod_{k=1}^{n} C_k\right]\times
    (e^{-\pi f t^*})\times
    FS}

where each term is calculated as follows:

1.  **Source moment (Nm/s)** (Eq.~7.24): The source term is the moment-rate function,

  .. math::

    \dot{M}(t)=M_0\,\dot{s}(t),

  where :math:`s(t)` is the normalized source-time function. Here, we only use the scalar seismic moment, computed as:

  .. math::

    M_0 = 10^{\,1.5M_w + 9.1} \; \mathrm{N\,m}.

2. **Radiation pattern (dimensionless)**, :math:`F` (Eqs.~17.71--17.73):

  .. math::

    \begin{aligned}
    F_P ={}&
    \left[\cos(\lambda)\sin(\delta)\sin2(\phi_{r}-\phi_{f})
    -
    \sin(\lambda)\sin(2\delta)\sin^2(\phi_{r}-\phi_{f})\right]
    \sin^2(i_h)
    \\
    &+
    \left[\sin(\lambda)\cos(2\delta)\sin(\phi_{r}-\phi_{f})
    -
    \cos(\lambda)\cos(\delta)\cos(\phi_{r}-\phi_{f})\right]
    \sin(2i_h)
    \\
    &+
    \sin(\lambda)\sin(2\delta)\cos^2(i_h),\\ \\
    F_{SV} ={}&
    \left[\sin\lambda\,\cos(2\delta)\sin(\phi_{r}-\phi_{f})
    -\cos\lambda\,\cos\delta\,\cos(\phi_{r}-\phi_{f})\right]\cos(2i_h)
    \\
    &+
    \frac{1}{2}\cos\lambda\,\sin\delta\,\sin2(\phi_{r}-\phi_{f})\sin(2i_h)
    \\
    &-
    \frac{1}{2}\sin\lambda\,\sin(2\delta)
    \left[1+\sin^2(\phi_{r}-\phi_{f})\right],\\\\
    F_{SH} ={}&
    \left[\cos\lambda\,\cos\delta\,\sin(\phi_{r}-\phi_{f})
    +\sin\lambda\,\cos(2\delta)\cos(\phi_{r}-\phi_{f})\right]\cos i_h
    \\
    &+
    \left[\cos\lambda\,\sin\delta\,\cos2(\phi_{r}-\phi_{f})
    -\frac{1}{2}\sin\lambda\,\sin(2\delta)\sin2(\phi_{r}-\phi_{f})\right]\sin i_h.\\
    \end{aligned}


  where,

  .. math::

    i_h = \text{takeoff},
    \qquad
    \phi_r = \text{azimuth},


    \phi_f = \text{strike},
    \qquad
    \delta = \text{dip},
    \qquad
    \lambda = \text{rake}.

3. **Radiation term** ( :math:`\text{kgs}^{-3}` ), :math:`R_s` (Eq.~17.70):

  .. math::

        R_s = 4\pi \rho_s V_s^3 \times 10^{12},


  where, :math:`\rho_s` and :math:`V_s` are density and velocity at source,
  respectively. Conversion factor of :math:`10^{12}` for changing density from
  :math:`\text{g } \text{cm}^{-3}` to :math:`\text{kg } \text{m}^{-3}` and
  velocity from :math:`\text{km } \text{s}^{-1}` to :math:`\text{m } \text{s}^{-3}`.

4. **Geometrical spreading (1/km)**, :math:`G(\Delta,h)` (Eqs.~13.9):

  .. math::

        G(\Delta,h)=  \sqrt{\frac{E(\Delta)}{E_h}}

  .. math::

        E(\Delta)=
        E_h
        \left(\frac{\sin i_h}{r_0^2 \cos i_0 \sin\Delta}\right)
        \left(\frac{di_h}{|d\Delta|}\right),

  where :math:`E(\Delta)` is the seismic energy density (energy per unit area)
  at epicentral distance :math:`\Delta`, :math:`E_h=K/2\pi` is the source
  energy density on the initial hemispherical wavefront with total radiated
  energy :math:`K`, :math:`r_h` and :math:`r_0` are the source and receiver
  radii from the Earth's center, respectively, :math:`i_h` and :math:`i_0`
  are the takeoff and incidence angles, :math:`p` is the ray parameter, and
  :math:`c_h` is the P- or S-wave velocity at the source.

5. **Reflection and transmission coefficients (dimensionless)**, :math:`C_k`.

  The product is over all discontinuities encountered over the phase path.
  The four interface types considered are summarized in Tables S1--S4.

  .. math::

        \prod_{k=1}^{n} C_k

6. **Attenuation (dimensionless)** (Eq.~B13.2.2):\\

  .. math::

     \text{Attenuation} = e^{-\pi f t^*}


  where the integrated attenuation along the ray path is

  .. math::

    t^*
    =
    \int_{\mathrm{path}} \frac{dt}{Q}
    =
    \sum_{i=1}^{N} \frac{t_i}{Q_i},

  with :math:`Q_i` and :math:`t_i` denoting the quality factor and travel time
  through the :math:`i`-th layer, respectively.

7. **Free-surface correction (dimensionless)**, :math:`FS` (Eqs.~13.66--13.68):\\


  .. math::

    FS_r^{(P)}
    =
    \frac{4p\,\alpha\,\eta_\alpha\eta_\beta \beta^{-2}}{A},
    \qquad
    FS_z^{(P)}
    =
    \frac{2\alpha\,\eta_\alpha E\beta^{-2}}{A}



  .. math::

    FS_r^{(SV)}
    =
    \frac{2\eta_\beta E\beta^{-1}}{A},
    \qquad
    FS_z^{(SV)}
    =
    -\frac{
    4p\,\eta_\alpha\eta_\beta\beta^{-1}}{A}.

  where,

  .. math::

    \eta_\alpha=\sqrt{\frac{1}{\alpha^2}-p^2},
    \qquad
    \eta_\beta=\sqrt{\frac{1}{\beta^2}-p^2},
    \qquad
    A=4p^2\eta_\alpha\eta_\beta+\left(\frac{1}{\beta^{2}}-2p^2\right)^2,
    \qquad
    E=\eta_\beta^2-p^2.

  :math:`\rho` is the density,
  :math:`\alpha` and :math:`\beta` are the P- and S-wave velocities, respectively.

  Eq. 13.66 in :cite:t:`fmgs` should not have the
  term :math:`(\eta_\beta^2-p^2)` and Eq. 13.67 is missing a a factor of 2 times
  for the ray parameter.

S1. Solid-solid

  Solid-solid displacement reflection and transmission coefficients.

  The P--SV System

  .. math::

    R_{PP} &= [(b\eta_{\alpha1}-c\eta_{\alpha2})F
    -(a+d\eta_{\alpha1}\eta_{\beta2})Hp^{2}]/D

    R_{PS} &=
    -2\eta_{\alpha1}\left(ab+cd\,\eta_{\alpha2}\eta_{\beta2}
    \right)p({\alpha_1}/{\beta_1})
    /{D}

    T_{PP} &= {
    2\rho_1\eta_{\alpha1}F
    \left({\alpha_1}/{\alpha_2}\right)
    }/{D}

    T_{PS} &= {
    2\rho_1\eta_{\alpha1}Hp
    \left({\alpha_1}/{\beta_2}\right)}/{D}

    R_{SS} &= {-[
    \left(b\eta_{\beta1}-c\eta_{\beta2}\right)E
    - \left(a+d\eta_{\alpha2}\eta_{\beta1}\right)Gp^{2}
    ]}/{D}

    R_{SP} &= {
    -2\eta_{\beta1}\left(ab+cd\,\eta_{\alpha2}\eta_{\beta2}\right)
    p\left({\beta_1}/{\alpha_1}\right)
    }/{D}

    T_{SP} &= {
    -2\rho_1\eta_{\beta1}Gp({\beta_1}/{\alpha_2})
    }/{D}

    T_{SS} &= {
    2\rho_1\eta_{\beta1}E
    ({\beta_1}/{\beta_2})
    }/{D}

  The SH System

  .. math::

    R_{SS} &= {
    (\mu_1\eta_{\beta1}-\mu_2\eta_{\beta2}})/
    ({\mu_1\eta_{\beta1}+\mu_2\eta_{\beta2}
    })

    T_{SS} &= {
    (2\mu_1\eta_{\beta1}})/
    ({\mu_1\eta_{\beta1}+\mu_2\eta_{\beta2}})

  where,

  .. math::

    \eta_{\alpha} &= \sqrt{\frac{1}{\alpha^{2}}-p^{2}}

    \mu_1 &= \rho_1\beta_1^2

    a &= \rho_2\left(1-2\beta_2^2p^2\right)
         -\rho_1\left(1-2\beta_1^2p^2\right),

    b &= \rho_2\left(1-2\beta_2^2p^2\right)
         +2\rho_1\beta_1^2p^2,

    c &= \rho_1\left(1-2\beta_1^2p^2\right)
         +2\rho_2\beta_2^2p^2,

    d &= 2\left(\rho_2\beta_2^2-\rho_1\beta_1^2\right)

    \eta_{\beta} &= \sqrt{\frac{1}{\beta^{2}}-p^{2}}

    \mu_2 &= \rho_2\beta_2^2

    E &= b\eta_{\alpha1}+c\eta_{\alpha2},

    F &= b\eta_{\beta1}+c\eta_{\beta2},

    G &= a-d\eta_{\alpha1}\eta_{\beta2},

    H &= a-d\eta_{\alpha2}\eta_{\beta1},

    D &= EF+GHp2

    \rho, \alpha,\beta: \text{density, P- and S-wave velocities}

  Corrections to Table 13.1 of :cite:t:`fmgs`:

  In the P-SV system, the :math:`R_{SS}` expression in :cite:t:`fmgs` contains :math:`b\eta_{\alpha2}\eta_{\beta1}`, which should instead be :math:`d\eta_{\alpha2}\eta_{\beta1}`.

  Similarly, the :math:`T_{SS}` expression contains :math:`2\rho_2\eta_{\beta1}`, which should instead be :math:`2\rho_1\eta_{\beta1}`.


S2. Fluid-fluid displacement reflection and transmission coefficients.

  Incident P

    .. math::

      R_{PP}
       =
       {{
      [\rho_1\eta_{\alpha2}
      -
      \rho_2\eta_{\alpha1}
      }]/{D_{ff}}}

      T_{PP}
       =
       {2\rho_1
      \left({\alpha_1}{\alpha_2^{-1}}\right)
      \eta_{\alpha1}
      }/{D_{ff}}

  where,

    .. math::

       D_{ff}=
      \rho_2\eta_{\alpha1}
      +
      \rho_1\eta_{\alpha2}.


S3. Solid-fluid and Fluid-solid displacement reflection and transmission coefficients.

Incident P--SV  From Solid (Medium 1)


  .. math::

    R_{PP}
      =
    {[\eta_{\alpha1}
    \left(
    4\beta_1^{4}p^{2}\rho_1\eta_{\alpha2}\eta_{\beta1}
    +\rho_2\right)
    -
    \rho_1\eta_{\alpha2}
    C_{sf}^{2}
    }]/{D_{sf}}

    R_{PS}
      =
    {-4\alpha_1\beta_1
    p\rho_1
    \eta_{\alpha1}\eta_{\alpha2}
    C_{sf}
    }/{D_{sf}}

    T_{PP}
      =
    {
    2\left({\alpha_1}/{\alpha_2}\right)
    \rho_1\eta_{\alpha1}
    C_{sf}
    }/{D_{sf}}

    R_{SS}
     =
    [{
    \rho_1\eta_{\alpha2}
    C_{sf}^{2}
    +
    \eta_{\alpha1}
    \left(
    \rho_2
    -
    4\beta_1^{4}p^{2}\rho_1
    \eta_{\alpha2}\eta_{\beta1}
    \right)
    }]
    /{D_{sf}}

    R_{SP}
     =
    {4\alpha_1^{-1}\beta_1^{3}\rho_1
    p
    \eta_{\alpha2}\eta_{\beta1}
    C_{sf}
    }
    /{D_{sf}}

    T_{SP}
     =
    {
    4\alpha_2^{-1}\beta_1^{3}\rho_1
    p
    \eta_{\alpha1}\eta_{\beta1}
    }
    /{D_{sf}}

Incident SH  From Solid (Medium 1)

  .. math::

    R_{ss}  =  1

Incident P  From Fluid (Medium 1)

  .. math::

    R_{PP}
      =
    {{
    \left[
    -\rho_2\eta_{\alpha1}
    \left(
    4\beta_2^{4}p^{2}\eta_{\alpha2}\eta_{\beta2}
    +C_{fs}^{2}
    \right)
    +\rho_1\eta_{\alpha2}
    \right]
    (\alpha_2^{2}\beta_2^{-1}\rho_1^{-1})
    }
    /{D_{fs}}}

    T_{PP}
      =
    {
    -2\alpha_1\alpha_2
    \eta_{\alpha1}
    C_{fs}
    \beta_2^{-1}
    }
    /{D_{fs}}

    T_{PS}  =
    {4\alpha_1\alpha_2^{2}
    p
    \eta_{\alpha1}
    \eta_{\alpha2}
    /D_{fs}}

  where,

  .. math::

    D_{sf} = \rho_2\eta_{\alpha1}
    +\rho_1\eta_{\alpha2}
    [4\beta_1^{4}p^{2}
    \eta_{\alpha1}\eta_{\beta1}
    +(C_{sf})^{2}]

    C_{sf} = 1-2\beta_1^{2}p^{2}

    D_{fs} =
    -{\alpha_2^{2}}{(\beta_2\rho_1)^{-1}}
    \Big[
    4\rho_2\beta_2^{4}p^{2}
    \eta_{\alpha1}\eta_{\alpha2}\eta_{\beta2}
    +\rho_2C_{fs}^{2}\eta_{\alpha1}
    +2\rho_1\beta_2^{2}p^{2}\eta_{\alpha2}
    +\rho_1C_{fs}\eta_{\alpha2}
    \Big]

    C_{fs} = 1-2\beta_2^{2}p^{2}


  The fluid-to-solid coefficients here are significantly different from
  Table 13.2 in :cite:t:`fmgs`. We show our calculations in the
  Jupyter notebook refltrans.ipynb in the Zenodo repository
  \citep{crotwell_2026_22086196}.

S4. Free-surface displacement reflection and transmission coefficients.

Incident P--SV  From Solid

  .. math::

    R_{PP}
     =
     {\Big[
    -\left({\beta^{-2}}-2p^2\right)^2
    +4p^2\eta_{\alpha}\eta_{\beta}
    }\Big]/{D_{sfree}}

    R_{PS}
     =
    {{
    4\left({\alpha}{\beta^{-1}}\right)
    p\,\eta_{\alpha1}
    \left({\beta^{-2}}-2p^2\right)
    }/{D_{sfree}}}

    R_{SP}
    =
    {{
    4\left({\beta}{\alpha^{-1}}\right)
    p\,\eta_{\beta}
    \left({\beta^{-2}}-2p^2\right)
    }/{D_{sfree}}}

    R_{SS}
    =
    {\Big[({\beta^{-2}}-2p^2)^2
    -
    4p^2\eta_{\alpha}\eta_{\beta}
    }\Big]/ {D_{sfree}}


Incident SH  From Solid

  .. math::

    R_{ss} = 1

Incident P  From Fluid

  .. math::

    R_{pp} = 1

where,

  .. math::

    D_{sfree} =
    \left({\beta^{-2}}-2p^2\right)^2
    +
    4p^2\eta_{\alpha}\eta_{\beta}


Corrections in Table 13.3 :cite:t:`fmgs`
:math:`R_{ss}` is missing a multiplication factor of -1.
