# video thing stream upload to offsite

## rsync notes
Wasn't working, I dunno

## S3 notes
Multipart is our *exact* usecase: https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html
multipart in android: https://github.com/jgilfelt/android-simpl3r
THe downside is it's locked into a cloud provider instead of a user-controlled server
