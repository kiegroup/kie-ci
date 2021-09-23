# KIE JENKINS SCRIPTS

This repository is used to store [KIE](https://www.kie.org/) either provisioned machines ansible configuration and job DSLs for jenkins.

## Provisioned Machines ansible configuration

We have different kind of machines:
- KIE RHEL
- [smee](https://smee.io/) client
- [verdaccio](https://verdaccio.org/) service

### To test ansible playbooks

Just execute

```
docker run --name ansible-managed-node --privileged -p 2222:22 -d -it chusiang/ansible-managed-node:centos-7 /usr/sbin/init
ansible-playbook -e ansible_ssh_pass=root -e encrypted_password=root_pass kie-rhel.yml
```


