package fi.vm.sade.hakuperusteet.admin

trait SwaggerRedirect { self: AdminServlet =>
  val basePath = cfg.getString("hakuperusteetadmin.url.base")

  get("/swagger") {
    checkAuthentication()
    redirect(basePath + "/swagger-ui/2.1.8-M1/index.html?url=" + basePath + "/api-docs")
  }

}
