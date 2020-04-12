package org.gubanov.app

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConfigurationProperties("app")
@ConstructorBinding
class AppConfig(val authenticationDuration: Duration)