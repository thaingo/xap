#!/usr/bin/env bash
echo "Undeploying services (processing units)..."
./undeploy.sh

echo "Killing GSCs with zones {{project.artifactId}}-space, {{project.artifactId}}-mirror"
../gs.sh container kill --zones={{project.artifactId}}-space,{{project.artifactId}}-mirror

{{#db.demo.enabled}}
echo "Stopping HSQL DB..."
./demo-db/shutdown.sh
{{/db.demo.enabled}}

echo "Demo stop completed"