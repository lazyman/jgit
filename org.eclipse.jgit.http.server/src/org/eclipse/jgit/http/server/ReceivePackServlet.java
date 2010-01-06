/*
 * Copyright (C) 2009, Google Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.http.server;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.eclipse.jgit.http.server.ServletUtils.getInputStream;
import static org.eclipse.jgit.http.server.ServletUtils.getRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jgit.http.server.resolver.ReceivePackFactory;
import org.eclipse.jgit.http.server.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.http.server.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser.PacketLineOutRefAdvertiser;

/** Server side implementation of smart push over HTTP. */
class ReceivePackServlet extends HttpServlet {
	private static final String REQ_TYPE = "application/x-git-receive-pack-request";

	private static final String RSP_TYPE = "application/x-git-receive-pack-result";

	private static final long serialVersionUID = 1L;

	static class InfoRefs extends SmartServiceInfoRefs {
		private final ReceivePackFactory receivePackFactory;

		InfoRefs(final ReceivePackFactory receivePackFactory) {
			super("git-receive-pack");
			this.receivePackFactory = receivePackFactory;
		}

		@Override
		protected void advertise(HttpServletRequest req, Repository db,
				PacketLineOutRefAdvertiser pck) throws IOException,
				ServiceNotEnabledException, ServiceNotAuthorizedException {
			receivePackFactory.create(req, db).sendAdvertisedRefs(pck);
		}
	}

	private final ReceivePackFactory receivePackFactory;

	ReceivePackServlet(final ReceivePackFactory receivePackFactory) {
		this.receivePackFactory = receivePackFactory;
	}

	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse rsp) throws IOException {
		if (!REQ_TYPE.equals(req.getContentType())) {
			rsp.sendError(SC_UNSUPPORTED_MEDIA_TYPE);
			return;
		}

		final Repository db = getRepository(req);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			final ReceivePack rp = receivePackFactory.create(req, db);
			rp.setBiDirectionalPipe(false);
			rp.receive(getInputStream(req), out, null);

		} catch (ServiceNotAuthorizedException e) {
			rsp.sendError(SC_UNAUTHORIZED);
			return;

		} catch (ServiceNotEnabledException e) {
			rsp.sendError(SC_FORBIDDEN);
			return;

		} catch (IOException e) {
			getServletContext().log("Internal error during receive-pack", e);
			rsp.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}

		reply(rsp, out.toByteArray());
	}

	private void reply(final HttpServletResponse rsp, final byte[] result)
			throws IOException {
		rsp.setContentType(RSP_TYPE);
		rsp.setContentLength(result.length);
		final OutputStream os = rsp.getOutputStream();
		try {
			os.write(result);
			os.flush();
		} finally {
			os.close();
		}
	}
}