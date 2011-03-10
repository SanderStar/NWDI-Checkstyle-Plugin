package org.arachna.netweaver.nwdi.checkstyle;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.arachna.netweaver.hudson.util.FilePathHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 * 
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link CheckstyleBuilder} is created. The created instance is persisted to
 * the project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 * 
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 * @author Kohsuke Kawaguchi
 */
public class CheckstyleBuilder extends Builder {
    /**
     * Descriptor for {@link CheckstyleBuilder}.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final String CHECKSTYLE_CONFIG_XML = "checkstyle-config.xml";
    private final String name;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public CheckstyleBuilder(final String name) {
        this.name = name;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        // this is where you 'build' the project
        // since this is a dummy, we just say 'hello world' and call that a
        // build

        // this also shows how you can consult the global configuration of the
        // builder
        boolean result = true;

        try {
            final NWDIBuild nwdiBuild = (NWDIBuild)build;
            final Collection<DevelopmentComponent> components = nwdiBuild.getAffectedDevelopmentComponents();
            nwdiBuild.getWorkspace().child(CHECKSTYLE_CONFIG_XML)
                .write(this.getDescriptor().getConfiguration(), "UTF-8");

            final String pathToWorkspace = FilePathHelper.makeAbsolute(nwdiBuild.getWorkspace());

            final CheckStyleExecutor executor =
                new CheckStyleExecutor(pathToWorkspace, new File(pathToWorkspace + File.separatorChar
                    + CHECKSTYLE_CONFIG_XML));

            for (final DevelopmentComponent component : components) {
                executor.execute(component);
            }
        }
        catch (final IOException e) {
            e.printStackTrace(listener.getLogger());
            result = false;
        }
        catch (final InterruptedException e) {
            // finish.
        }

        return result;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor for {@link CheckstyleBuilder}. Used as a singleton. The class
     * is marked as public so that it can be accessed from views.
     * 
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension(ordinal = 1000)
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Persistent checkstyle configuration.
         * 
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String configuration;

        // TODO: add excludes (file name pattern and contains selectors)

        /**
         * Create descriptor for NWDI-CheckStyle-Builder and load global
         * configuration data.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckConfiguration(@QueryParameter final String value) throws IOException,
            ServletException {
            return value.length() == 0 ? FormValidation.error("Please insert a checkstyle configuration.")
                : FormValidation.ok();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            System.err.println(aClass.getName());
            return NWDIProject.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "NWDI Checkstyle";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            final String fileKey = formData.getString("checkStyleConfiguration");

            try {
                final FileItem item = req.getFileItem(fileKey);

                if (item != null) {
                    final String content = item.getString();

                    if (content != null && content.trim().length() > 0) {
                        this.configuration = content;
                    }
                }
            }
            catch (final ServletException e) {
                throw new FormException(e, "ServletException");
            }
            catch (final IOException e) {
                throw new FormException(e, "IOException");
            }

            save();

            return super.configure(req, formData);
        }

        /**
         * Return the checkstyle configuration.
         */
        public String getConfiguration() {
            return configuration;
        }

        /**
         * @param configuration
         *            the configuration to set
         */
        public void setConfiguration(final String configuration) {
            this.configuration = configuration;
        }
    }
}
