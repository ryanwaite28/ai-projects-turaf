# AWS Accounts

This document outlines the AWS accounts used in this project.


## Root Organization
- org ID: `o-l3zk5a91yj`
- ARN: `arn:aws:organizations::072456928432:root/o-l3zk5a91yj/r-gs6r`
- root ID: `r-gs6r`



| Account Name | Account ID | ARN | Email | Purpose |
|--------------|------------|---------|---------|---------|
| root | 072456928432 | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/072456928432` | aws@turafapp.com | Original, Management Account |
| Ops | 146072879609 | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/146072879609` | aws-ops@turafapp.com | Operations/DevOps Account |
| dev | 801651112319 | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/801651112319` | aws-dev@turafapp.com | Development Account |
| qa | 965932217544 | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/965932217544` | aws-qa@turafapp.com | Quality Assurance Account |
| prod | 811783768245 | `arn:aws:organizations::072456928432:account/o-l3zk5a91yj/811783768245` | aws-prod@turafapp.com | Production Account |