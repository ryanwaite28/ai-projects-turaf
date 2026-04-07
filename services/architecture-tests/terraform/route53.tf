data "aws_route53_zone" "main" {
  name         = "turafapp.com"
  private_zone = false
}

resource "aws_route53_record" "reports" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "reports.${var.environment}.turafapp.com"
  type    = "A"
  
  alias {
    name                   = aws_cloudfront_distribution.test_reports.domain_name
    zone_id                = aws_cloudfront_distribution.test_reports.hosted_zone_id
    evaluate_target_health = false
  }
}
