require 'seis_tasks'
build = seis_default 'TauP', '1.2beta', ['../seisFile/exportedJars.yaml']
directory "#{class_dir(build)}/edu/sc/seis/TauP/StdModels"

task :modelCopy => "#{class_dir(build)}/edu/sc/seis/TauP/StdModels" do |t|
    safe_ln(FileList['StdModels/*'].to_a, t.prerequisites[0])
end

task output_jar(build) => :modelCopy