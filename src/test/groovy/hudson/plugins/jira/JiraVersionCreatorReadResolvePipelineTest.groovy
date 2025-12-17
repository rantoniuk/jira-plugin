package hudson.plugins.jira

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JiraVersionCreatorReadResolvePipelineTest extends BasePipelineTest {

    @BeforeEach
    void setUpTest() {
        super.setUp()
        helper.registerAllowedMethod("node", [Closure]) { Closure body ->
            body.call()
        }
        helper.registerAllowedMethod("jiraCreateVersion", [Map]) { Map args ->
            def builder = new JiraVersionCreatorBuilder(
                    args.jiraVersion as String,
                    args.jiraProjectKey as String)
            nullifyFailIfAlreadyExists(builder)
            def resolved = builder.readResolve()
            assert resolved instanceof JiraVersionCreatorBuilder
            assert resolved.isFailIfAlreadyExists()
        }
        helper.registerAllowedMethod("step", [Map]) { Map args ->
            if (args?.$class == 'JiraVersionCreator') {
                def notifier = new JiraVersionCreator(
                        args.jiraVersion as String,
                        args.jiraProjectKey as String)
                nullifyFailIfAlreadyExists(notifier)
                def resolved = notifier.readResolve()
                assert resolved instanceof JiraVersionCreator
                assert resolved.isFailIfAlreadyExists()
            }
        }
    }

    @Test
    void pipelineBuilderReadResolve() {
        def script = loadScript("pipelines/JiraVersionCreatorBuilderPipeline.groovy")
        script.run()
        assertJobStatusSuccess()
    }

    @Test
    void pipelineNotifierReadResolve() {
        def script = loadScript("pipelines/JiraVersionCreatorNotifierPipeline.groovy")
        script.run()
        assertJobStatusSuccess()
    }

    private static void nullifyFailIfAlreadyExists(Object target) {
        def field = target.getClass().getDeclaredField("failIfAlreadyExists")
        field.accessible = true
        field.set(target, null)
    }
}
