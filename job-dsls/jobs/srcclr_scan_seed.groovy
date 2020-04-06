import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

String jobDescription = "Job responsible for SourceClear verification"

def jobDefinition = job("srcclr-scan") {


    parameters {
        choiceParam('SCAN_TYPE', ['scm', 'binary'])
        stringParam('URL','')
        stringParam('VERSION', '')
        stringParam('PACKAGE','')
        stringParam('NAME', '')
        stringParam('MVNPARAMS', '')
        choiceParam('PROCESSOR_TYPE', ['cve', 'cvss'])
        booleanParam('RECURSIVE', false)
        booleanParam('DEBUGGING', false)
        booleanParam('TRACING', false)
        stringParam('SCMVERSION', '')
        stringParam('THRESHOLD', '1','Threshold from 1 to 10 for cvss processor')
    }


    environmentVariables{
        groovy('''
          def map = [:]
               
          map.put("RECURSE", Boolean.valueOf("${RECURSIVE}") ? "--recursive" : "")
          
          map.put("DEBUG", Boolean.valueOf("${DEBUGGING}") ? "-d" : "");
          
          if ( "${TRACING}" == "true" ) {
            
            map.put("TRACE"," --trace ")
          
          } else {
            
            map.put("TRACE","")
            
          }
          
          map.put("TRACE", Boolean.valueOf("${TRACING}") ? "--trace" : "")
          
          if ( "${MVNPARAMS}" != "" )
          {
          
            map.put("MVNPARAMETER","--maven-param=\\"${MVNPARAMS}\\"")
            
          } else {
          
            map.put("MVNPARAMETER", "")
            
          }
          
          if ( "${SCAN_TYPE}" == "scm" ) {
          
            map.put("SCMVERSIONPARAM"," --ref=${SCMVERSION}")
            
          } else {
            map.put("SCMVERSIONPARAM","")
          }
          
          return map
          
          return []
           
        ''')
    }


    label("kie-rhel7")

    wrappers {
        credentialsBinding {
            string("SRCCLR_API_TOKEN", "SRCCLR_API_TOKEN")
        }
    }

    scm {
        git {
            remote {
                name('origin')
                url('https://github.com/project-ncl/sourceclear-invoker')
            }
            branch('master')
        }
    }
    steps {
        maven {
            mavenInstallation("kie-maven-3.5.4")
            goals("-Pjenkins test -Dmaven.buildNumber.skip=true -DargLine='' -Dsourceclear=\"\${DEBUG} \${TRACE} --processor=\${PROCESSOR_TYPE} --product-version=\${VERSION} --package=\${PACKAGE} --product=\"\${NAME}\" --threshold=\${THRESHOLD} \${SCAN_TYPE} --url=\${URL} \${MVNPARAMETER} \${SCMVERSIONPARAM} \${RECURSE}\"")
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/*.xml") {
            retainLongStdout()
            healthScaleFactor(1.0)
            allowEmptyResults()
        }
        archiveArtifacts {
            pattern("**/target/screenshots/*")
            allowEmpty()
        }
    }
}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)