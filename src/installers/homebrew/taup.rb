class Taup < Formula
  desc "Flexible Seismic Travel-Time and Raypath Utilities"
  homepage "https://www.seis.sc.edu/TauP/"
  url "https://zenodo.org/records/15426279/files/TauP-3.0.1.zip"
  sha256 "569a5e1d5d9268e57e6f08ace451946b8264474a951d64bf77283523bac3f1be"
  revision 1
  license "LGPL-3.0-or-later"
  depends_on "openjdk"

  def install
    rm Dir["bin/*.bat"]
    libexec.install %w[bin docs lib src]
    env = if Hardware::CPU.arm?
      Language::Java.overridable_java_home_env("11")
    else
      Language::Java.overridable_java_home_env
    end
    (bin/"taup").write_env_script libexec/"bin/taup", env
    cp "./taup_completion", "./taup_completion.bash"
    bash_completion.install "./taup_completion.bash"
    zsh_completion.install "./taup_completion" => "_taup"
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/taup --version")
    taup_output = shell_output("#{bin}/taup help")
    assert_includes taup_output, "Usage: taup"
  end
end
