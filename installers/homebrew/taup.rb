class Taup < Formula
  desc "Flexible Seismic Travel-Time and Raypath Utilities"
  homepage "https://www.seis.sc.edu/TauP/"
  url "https://www.seis.sc.edu/downloads/TauP/TauP-2.5.0.tgz"
  sha256 "5412c96516dab825e77791ed00c28d5ddcb2f66b5d19dd4aad8cbb60f36f1b66"
  license "LGPL-3.0-or-later"

  bottle :unneeded

  def install
    rm_f Dir["bin/*.bat"]
    rm_f Dir["bin/taup_*"]
    libexec.install %w[bin docs lib src]
    env = if Hardware::CPU.arm?
      Language::Java.overridable_java_home_env("11")
    else
      Language::Java.overridable_java_home_env
    end
    (bin/"taup").write_env_script libexec/"bin/taup", env
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/taup --version")
    taup_output = shell_output("#{bin}/taup help")
    assert_includes taup_output, "Usage: taup"
  end
end
