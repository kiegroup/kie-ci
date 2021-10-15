#!/bin/bash -ef

# expects two environment variables, source and target OpenStack cloud RC files to be sourced
SOURCE_CLOUD_RC=${SOURCE_CLOUD:?"You have to provide the source cloud RC file as the first argument"}
TARGET_CLOUD_RC=${TARGET_CLOUD:?"You have to provide the target cloud RC file as the second argument"}

echo "Copying image $IMAGE_NAME from source cloud ($SOURCE_CLOUD_RC) to target cloud ($TARGET_CLOUD_RC)."
echo "Already existing image with the same name will be renamed as a backup."

IMAGE_FILE="$WORKSPACE/$IMAGE_NAME"
BACKUP_SUFFIX="sync-backup"

# load the source cloud RC file
. $SOURCE_CLOUD_RC

# property hw_video_model=vga needed for Windows images to get high screen resolution
openstack image show -f shell -c properties $IMAGE_NAME | grep 'hw_video_model' || NO_HW_VIDEO_MODEL='true'

echo "NO_HW_VIDEO_MODEL: $NO_HW_VIDEO_MODEL"
DISK_FORMAT=$(openstack image show -f value -c disk_format $IMAGE_NAME)
echo "Determined disk format $DISK_FORMAT"

echo "Fetching the image from the source cloud to ${IMAGE_FILE}..."
openstack image save --file $IMAGE_FILE $IMAGE_NAME

# load the target cloud RC file
. $TARGET_CLOUD_RC

# check if a private image with the same name already exists, if so, rename it
IMAGE_ID=`openstack image list --private -f value -c ID --name $IMAGE_NAME`
if [ -n "$IMAGE_ID" ]
then
  BACKUP_NAME="$IMAGE_NAME-$BACKUP_SUFFIX"
  echo "Image $IMAGE_ID already exists, renaming to $BACKUP_NAME"
  openstack image set --name $BACKUP_NAME $IMAGE_ID
fi

if [ -n "$NO_HW_VIDEO_MODEL" ]
then
  echo "Creating new image (hw_video_model not set, disk format $DISK_FORMAT) in the target cloud..."
  openstack image create --private --disk-format $DISK_FORMAT --file $IMAGE_FILE $IMAGE_NAME
else
  echo "Creating new image (hw_video_model is set, disk format $DISK_FORMAT) in the target cloud..."
  openstack image create --private --property hw_video_model=vga --disk-format $DISK_FORMAT --file $IMAGE_FILE $IMAGE_NAME
fi

rm -f $IMAGE_FILE
