#!/bin/bash
awslocal s3 mb s3://clipsearch || true
awslocal sqs create-queue --queue-name clipsearch-index-queue || true
