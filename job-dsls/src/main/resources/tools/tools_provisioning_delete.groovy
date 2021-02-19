switch(Abr) {
    case "VerdaccioServ":
        return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"verdaccio-service-\$INSTANCE_NUMBER\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s\n"
        case "DockerReg":
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"provisioner-job-docker-registry-server\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s\n"
        case "SmeeClient":
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"smee-client-\$SMEE_NUMBER\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s"
        default:
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"provisioner-job-cekit-cacher-server\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s"
}
