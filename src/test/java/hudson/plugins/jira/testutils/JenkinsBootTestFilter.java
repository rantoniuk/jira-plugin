package hudson.plugins.jira.testutils;

import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.lang.reflect.AnnotatedElement;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Excludes tests already annotated (directly, or via an abstract superclass) with
 * {@code @WithJenkins}/{@code @WithJenkinsConfiguredWithCode} -- i.e. tests that boot a real
 * embedded Jenkins instance -- whenever the {@value #EXCLUDE_PROPERTY} system property is
 * {@code true}. See the {@code fast-tests} Maven profile in {@code pom.xml}, which sets that
 * property. When the property isn't set, every test is included, so default {@code mvn test}/
 * {@code mvn verify} behavior is unchanged.
 */
public class JenkinsBootTestFilter implements PostDiscoveryFilter {

    static final String EXCLUDE_PROPERTY = "jira.tests.excludeJenkins";

    @Override
    public FilterResult apply(TestDescriptor descriptor) {
        if (!Boolean.getBoolean(EXCLUDE_PROPERTY)) {
            return FilterResult.included(EXCLUDE_PROPERTY + " is not set");
        }
        return bootsJenkins(descriptor)
                ? FilterResult.excluded("annotated with @WithJenkins/@WithJenkinsConfiguredWithCode")
                : FilterResult.included("does not boot a Jenkins instance");
    }

    private static boolean bootsJenkins(TestDescriptor descriptor) {
        TestSource source = descriptor.getSource().orElse(null);
        if (source instanceof MethodSource methodSource) {
            return isAnnotated(methodSource.getJavaMethod()) || isAnnotated(methodSource.getJavaClass());
        }
        if (source instanceof ClassSource classSource) {
            return isAnnotated(classSource.getJavaClass());
        }
        return false;
    }

    private static boolean isAnnotated(AnnotatedElement element) {
        return AnnotationSupport.isAnnotated(element, WithJenkins.class)
                || AnnotationSupport.isAnnotated(element, WithJenkinsConfiguredWithCode.class);
    }
}
