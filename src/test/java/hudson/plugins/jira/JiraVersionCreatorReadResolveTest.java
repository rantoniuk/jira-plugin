package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.XStream2;
import org.jvnet.hudson.test.WithoutJenkins;
import org.junit.jupiter.api.Test;

class JiraVersionCreatorReadResolveTest {

    private final XStream2 xStream2 = new XStream2();

    @Test
    @WithoutJenkins
    void builderReadResolveSetsFailIfAlreadyExistsWhenMissingInConfig() {
        String xml = """
<hudson.plugins.jira.JiraVersionCreatorBuilder>
  <jiraVersion>1.0</jiraVersion>
  <jiraProjectKey>PROJ</jiraProjectKey>
</hudson.plugins.jira.JiraVersionCreatorBuilder>
""";
        JiraVersionCreatorBuilder builder = (JiraVersionCreatorBuilder) xStream2.fromXML(xml);

        assertTrue(builder.isFailIfAlreadyExists());
    }

    @Test
    @WithoutJenkins
    void notifierReadResolveSetsFailIfAlreadyExistsWhenMissingInConfig() {
        String xml = """
<hudson.plugins.jira.JiraVersionCreator>
  <jiraVersion>1.0</jiraVersion>
  <jiraProjectKey>PROJ</jiraProjectKey>
</hudson.plugins.jira.JiraVersionCreator>
""";
        JiraVersionCreator notifier = (JiraVersionCreator) xStream2.fromXML(xml);

        assertTrue(notifier.isFailIfAlreadyExists());
    }
}
