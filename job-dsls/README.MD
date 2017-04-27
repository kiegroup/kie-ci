# KIE Jenkins Job DSL  
 
## File structure

    .
    ├── jobs                    # DSL script files
    ├── resources               # resources for DSL scripts
    ├── src
    │   ├── main
    │   │   ├── groovy          # support classes
    │   │   └── resources
    │   │       └── idea.gdsl   # IDE support for IDEA
    │   └── test
    │       └── groovy          # specs (test files)
    └── build.gradle            # build file

## Testing

`./gradlew test` runs the specs.

[JobScriptsSpec](src/test/groovy/com/dslexample/JobScriptsSpec.groovy) 
will loop through all DSL files and make sure they don't throw any exceptions when processed. All XML output files are written to `build/debug-xml`. 
This can be useful if you want to inspect the generated XML before check-in.

## Seed Job

Manually create a job:

* Invoke Gradle script
   * Use Gradle Wrapper: `true`
   * Tasks: `clean test`
* Process Job DSLs
   * DSL Scripts: `job-dsls/jobs/**/*.groovy`
   * Additional classpath: `job-dsls/src/main/groovy`
* Publish JUnit test result report
   * Test report XMLs: `job-dsls/build/test-results/**/*.xml`

Note that starting with Job DSL 1.60 the "Additional classpath" setting is not available when
[Job DSL script security](https://github.com/jenkinsci/job-dsl-plugin/wiki/Script-Security) is enabled.
