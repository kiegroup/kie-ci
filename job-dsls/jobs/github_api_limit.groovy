import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

// Job Description
String jobDescription = "Job responsible for monitoring GitHub API request limit."

// Declare array of tokens for shell script
StringBuilder arrayDeclaration = new StringBuilder();

for (int i = 1; i <= Constants.NUMBER_OF_KIE_USERS.toInteger(); i++) {
    arrayDeclaration.append("tokensList[$i]=\$token$i\n")
}

String command = """#!/bin/bash +x
returnCode=0
${arrayDeclaration}

for i in {1..${Constants.NUMBER_OF_KIE_USERS}}
do
    limit=`curl -s "https://api.github.com/rate_limit?access_token=\${tokensList[\$iterator]}" | jq ".rate.remaining"`
    if [ limit -le 0 ]
    then
        echo "kie-ci\$i limit is exceeded - \$limit"
        returnCode=1
    else
        echo "kie-ci\$i limit is fine - \$limit"
    fi
done

exit \$returnCode
"""

// Creates or updates a free style job.
def jobDefinition = job("github-api-limit") {
    for ( i = 1; i <= Constants.NUMBER_OF_KIE_USERS.toInteger(); i++) {

        // Adds pre/post actions to the job.
        wrappers {

            // Binds environment variables to credentials.
            credentialsBinding {

                // Sets a variable to the text given in the credentials.
                string("token$i", "kie-ci$i-token")
            }
        }
    }

    // Label which specifies which nodes this job can run on.
    label("kie-linux&&kie-mem512m")

    //  This field follows the syntax of cron (with minor differences). Specifically, each line consists of 5 fields separated by TAB or whitespace
    cron("H/10 * * * *")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script.
        shell(command)
    }

    // Adds post-build actions to the job.
    publishers {
        // Sends email notifications.
        mailer('bxms-prod@redhat.com', false, false)
    }
}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)
