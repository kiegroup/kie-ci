#!/bin/bash -ef

# expects the environment variable specifying which OpenStack cloud RC file to be sourced
CLOUD_RC=${CLOUD:?"You have to provide the cloud RC file as the first argument"}

echo "Promoting image $IMAGE_NAME within cloud ($CLOUD_RC)."

IMAGE_FILE="$WORKSPACE/$IMAGE_NAME"

BACKUP_IMAGE_NAME="$IMAGE_NAME-fallback"
PRODUCTION_IMAGE_NAME="$IMAGE_NAME-latest"
CANDIDATE_IMAGE_NAME="$IMAGE_NAME-latest-new"

# load the cloud RC file
. $CLOUD_RC
# override OS_KEY_PATH to the Jenkins provided key
export OS_KEY_PATH="$HUDSON_KEY_PATH"

# check if a private image with the new candidate image already exists, if not, abort
IMAGE_ID=`openstack image list --private -f value -c ID --name "$CANDIDATE_IMAGE_NAME"`
if [ -z "$IMAGE_ID" ]
then
  echo "Candidate image $CANDIDATE_IMAGE_NAME does not exist, have you generated it? Aborting!"
  exit 1
fi

# check if a private image with the backup already exists, if so, delete it
IMAGE_ID=`openstack image list --private -f value -c ID --name "$BACKUP_IMAGE_NAME"`
if [ -n "$IMAGE_ID" ]
then
  echo "Backup image $BACKUP_IMAGE_NAME $IMAGE_ID already exists, deleting it"
  openstack image delete $IMAGE_ID
fi

# check if a private image with the production image already exists, if so, rename it to backup
IMAGE_ID=`openstack image list --private -f value -c ID --name "$PRODUCTION_IMAGE_NAME"`
if [ -n "$IMAGE_ID" ]
then
  echo "Production image $PRODUCTION_IMAGE_NAME $IMAGE_ID already exists, renaming to $BACKUP_IMAGE_NAME"
  openstack image set --name $BACKUP_IMAGE_NAME $IMAGE_ID
fi

# check if a private image with the new candidate image already exists, if so, rename it to production
IMAGE_ID=`openstack image list --private -f value -c ID --name "$CANDIDATE_IMAGE_NAME"`
if [ -n "$IMAGE_ID" ]
then
  echo "Candidate image $CANDIDATE_IMAGE_NAME $IMAGE_ID exists, renaming to production $PRODUCTION_IMAGE_NAME"
  openstack image set --name $PRODUCTION_IMAGE_NAME $IMAGE_ID
fi
