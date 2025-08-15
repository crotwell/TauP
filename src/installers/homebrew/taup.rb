class Taup < Formula
  desc "Flexible Seismic Travel-Time and Raypath Utilities"
  homepage "https://www.seis.sc.edu/TauP/"
  url "https://zenodo.org/records/16884103/files/TauP-3.1.0.zip"
  sha256 "0c4392867c0a8f9c876180a51fb5e1b81859a4c17fd2223efd0d41a573491538"
  license "LGPL-3.0-or-later"
  depends_on "openjdk"

  def install
    rm Dir["bin/*.bat"]
    libexec.install %w[bin docs lib src]
    env = Language::Java.overridable_java_home_env
    (bin/"taup").write_env_script libexec/"bin/taup", env
    cp "./taup_completion", "./taup_completion.bash"
    bash_completion.install "./taup_completion.bash"
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/taup --version")
    taup_output = shell_output("#{bin}/taup help")
    assert_includes taup_output, "Usage: taup"
  end
end
