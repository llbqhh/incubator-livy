/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.livy.server

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

private[livy] class AccessFilter(accessManager: AccessManager) extends Filter {

  override def init(filterConfig: FilterConfig): Unit = {}

  override def doFilter(request: ServletRequest,
                        response: ServletResponse,
                        chain: FilterChain): Unit = {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val remoteUser = httpRequest.getRemoteUser
    // 查看用户是否有权限试用livy的api
    if (accessManager.isUserAllowed(remoteUser)) {
      chain.doFilter(request, response)
    } else {
      val httpServletResponse = response.asInstanceOf[HttpServletResponse]
      httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
        "User not authorised to use Livy.")
    }
  }

  override def destroy(): Unit = {}
}

