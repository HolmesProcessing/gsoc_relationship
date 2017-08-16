package com.holmesprocessing.analytics.relationship.models

case class Record(object_id:String, pehash: Array[Byte], imphash: Array[Byte], binary_signature: Array[Byte],yara_rules: Array[Byte],domain_requests:Array[Byte], timestamp: java.util.UUID)

