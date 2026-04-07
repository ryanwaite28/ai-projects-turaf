resource "aws_cloudfront_origin_access_identity" "test_reports" {
  comment = "OAI for architecture test reports - ${var.environment}"
}

resource "aws_cloudfront_distribution" "test_reports" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "Architecture test reports CDN - ${var.environment}"
  default_root_object = "index.html"
  
  aliases = ["reports.${var.environment}.turafapp.com"]
  
  origin {
    domain_name = aws_s3_bucket.test_reports.bucket_regional_domain_name
    origin_id   = "S3-test-reports"
    
    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.test_reports.cloudfront_access_identity_path
    }
  }
  
  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-test-reports"
    
    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
    
    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
    compress               = true
  }
  
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  
  viewer_certificate {
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
  
  tags = {
    Name = "turaf-architecture-test-reports-${var.environment}"
  }
}

resource "aws_s3_bucket_policy" "test_reports" {
  bucket = aws_s3_bucket.test_reports.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_cloudfront_origin_access_identity.test_reports.iam_arn
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.test_reports.arn}/*"
      }
    ]
  })
}
