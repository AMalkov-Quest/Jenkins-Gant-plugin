package hudson.plugins.gant;

import hudson.tasks.Builder;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.CopyOnWrite;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Kohsuke Kawaguchi
 */
public class Gant extends Builder {
    /**
     * The targets, properties, and other Gant options.
     * Either separated by whitespace or newline.
     */
    private final String targets;

    /**
     * Identifies {@link GantInstallation} to be used.
     */
    private final String gantName;
    
    private final String scriptFile;
    private final String properties;

    @DataBoundConstructor
    public Gant(String targets,String gantName,String scriptFile, String properties) {
        this.targets = targets;
        this.gantName = gantName;
        this.scriptFile = scriptFile;
        this.properties = properties;
    }

    public String getTargets() {
        return targets;
    }
    
    public String getScriptFile() {
        return scriptFile;
    }
    
    public String getProperties() {
        return properties;
    }

    /**
     * Gets the Gant to invoke,
     * or null to invoke the default one.
     */
    public GantInstallation getGant() {
        for( GantInstallation i : DESCRIPTOR.getInstallations() ) {
            if(gantName!=null && i.getName().equals(gantName))
                return i;
        }
        return null;
    }
    
    /**
     * @return Parsed properties. Never null.
     * @throws java.io.IOException
     */
    public static Properties parseProperties(String properties) throws IOException {
        Properties props = new Properties();
        if (properties != null) {
            try {
                props.load(new StringReader(properties));
            } catch (NoSuchMethodError err) {
                props.load(new ByteArrayInputStream(properties.getBytes()));
            }
        }
        return props;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
    	VariableResolver<String> vr = build.getBuildVariableResolver();
    	ArgumentListBuilder args = new ArgumentListBuilder();

        GantInstallation ai = getGant();
        if( ai == null ) {
        	String execName;
            if(launcher.isUnix())
                execName = "gant";
            else
                execName = "gant.bat";
            args.add(execName);
        } else {
            File exec = ai.getExecutable();
            if(!ai.getExists()) {
                listener.fatalError(exec+" doesn't exist");
                return false;
            }
            args.add(exec.getPath());
        }
        
        if(scriptFile != null) {
        	args.add("-f");
        	args.add(scriptFile);
        }
        
        Map<String,String> env = build.getEnvironment(listener);
        if(ai != null) {
            if(ai.getGroovyHome() != null)
            	env.put("JAVA_HOME", ai.getJavaHome());
				
			if(ai.getGroovyHome() != null)
            	env.put("ANT_HOME", ai.getAntHome());
				
			if(ai.getGroovyHome() != null)
            	env.put("GROOVY_HOME", ai.getGroovyHome());
            
            if(ai.getScriptsHome() != null) {
	            env.put("SCRIPTS_HOME", ai.getScriptsHome());
	            args.add("-P");
	        	args.add(ai.getScriptsHome());
	        	args.add("-L");
	        	args.add(ai.getScriptsHome());
            }
        }
        
        args.addKeyValuePairs("-D",build.getBuildVariables());
        
        if(properties != null) {
            args.addKeyValuePairsFromPropertyString("-D",properties,vr);
         }

        if(targets != null) {
        	String normalizedTargets = targets.replaceAll("[\t\r\n]+"," ");
        	//skip targets that are marked with #
        	String onlyNotCommentedTargets = normalizedTargets.replaceAll("#[^\\s]+","");
        	args.addTokenized(onlyNotCommentedTargets);
        }

        try {
            int r = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(build.getModuleRoot()).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace( listener.fatalError("command execution failed") );
            return false;
        }
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Builder> {
        @CopyOnWrite
        private volatile GantInstallation[] installations = new GantInstallation[0];

        private DescriptorImpl() {
            super(Gant.class);
            load();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gant/help.html";
        }

        public String getDisplayName() {
            return "Invoke Gant script";
        }

        public GantInstallation[] getInstallations() {
            return installations;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            installations = req.bindParametersToList(
                GantInstallation.class,"gant.").toArray(new GantInstallation[0]);
            save();
            return true;
        }

    //
    // web methods
    //
        /**
         * Checks if the GROOVY_HOME is valid.
         */
        public FormValidation doCheckGroovyHome(@QueryParameter final String value) {
            // this can be used to check the existence of a file on the server, so needs to be protected
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) return FormValidation.ok();

            File f = new File(Util.fixNull(value));
            if(!f.isDirectory()) {
                return FormValidation.error(f+" is not a directory");
            }

            if(!new File(f,"bin/groovy").exists() && !new File(f,"bin/groovy.bat").exists()) {
                return FormValidation.error(f+" doesn't look like a Groovy directory");
            }

            if(!new File(f,"bin/gant").exists() && !new File(f,"bin/gant.bat").exists()) {
                return FormValidation.error(f+" looks like a Groovy but Gant is not found in here");
            }

            return FormValidation.ok();
        }
    }

}
