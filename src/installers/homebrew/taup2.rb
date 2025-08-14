class Taup2 < Formula
  desc "Flexible Seismic Travel-Time and Raypath Utilities"
  homepage "https://www.seis.sc.edu/TauP/"
  url "https://zenodo.org/records/10794858/files/TauP-2.6.1.tgz?download=1"
  sha256 "4041e3e9cbbbfde4196723b2e7e81f30800370056912b6be664a070526d99494"
  license "LGPL-3.0-or-later"
  revision 3
  depends_on "openjdk@11"

  def install
    rm Dir["bin/*.bat"]
    rm Dir["bin/taup_*"]
    libexec.install %w[bin docs lib src]
    env = Language::Java.overridable_java_home_env
    (bin/"taup2").write_env_script libexec/"bin/taup", env
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/taup --version")
    taup_output = shell_output("#{bin}/taup help")
    assert_includes taup_output, "Usage: taup"
  end
end
