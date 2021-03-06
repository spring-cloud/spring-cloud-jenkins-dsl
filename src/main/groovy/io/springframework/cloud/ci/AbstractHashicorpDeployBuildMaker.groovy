package io.springframework.cloud.ci

import groovy.transform.PackageScope
import io.springframework.cloud.common.HashicorpTrait
import io.springframework.cloud.common.SpringCloudJobs
import io.springframework.cloud.common.SpringCloudNotification
import io.springframework.common.Cron
import io.springframework.common.JdkConfig
import io.springframework.common.TestPublisher
import javaposse.jobdsl.dsl.DslFactory

/**
 * @author Marcin Grzejszczak
 */
@PackageScope
abstract class AbstractHashicorpDeployBuildMaker implements SpringCloudNotification, JdkConfig, TestPublisher, HashicorpTrait,
		Cron, SpringCloudJobs {
	protected final DslFactory dsl
	protected final String organization
	protected final String project

	AbstractHashicorpDeployBuildMaker(DslFactory dsl, String organization, String project) {
		this.dsl = dsl
		this.organization = organization
		this.project = project
	}

	void deploy() {
		dsl.job("$project-ci") {
			triggers {
				cron everyThreeHours()
				githubPush()
			}
			parameters {
				stringParam(branchVar(), masterBranch(), 'Which branch should be built')
			}
			jdk jdk8()
			scm {
				git {
					remote {
						url "https://github.com/${organization}/${project}"
						branch "\$${branchVar()}"
					}
				}
			}
			steps {
				shell(cleanup())
				shell(buildDocsWithGhPages())
				shell("""\
						${preStep()}
						${cleanAndDeploy()} || ${postStep()}
					""")
				shell postStep()
			}
			configure {
				slackNotificationForSpringCloud(it as Node)
			}
			publishers {
				archiveJunit mavenJUnitResults()
			}
		}
	}

	protected abstract String preStep()
	protected abstract String postStep()
}
