package org.obm.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IBackendFactory;
import org.obm.push.impl.FolderSyncHandler;
import org.obm.push.impl.GetItemEstimateHandler;
import org.obm.push.impl.HintsLoader;
import org.obm.push.impl.IRequestHandler;
import org.obm.push.impl.PingHandler;
import org.obm.push.impl.ProvisionHandler;
import org.obm.push.impl.Responder;
import org.obm.push.impl.SettingsHandler;
import org.obm.push.impl.SyncHandler;
import org.obm.push.store.IStorageFactory;
import org.obm.push.store.ISyncStorage;
import org.obm.push.utils.Base64;
import org.obm.push.utils.DOMUtils;
import org.obm.push.utils.FileUtils;
import org.obm.push.utils.RunnableExtensionLoader;
import org.obm.push.wbxml.WBXMLTools;
import org.w3c.dom.Document;

/**
 * ActiveSync server implementation. Routes all request to appropriate request
 * handlers.
 * 
 * @author tom
 * 
 */
public class ActiveSyncServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4136686109306545436L;

	private static final Log logger = LogFactory
			.getLog(ActiveSyncServlet.class);

	private Map<String, IRequestHandler> handlers;
	private Map<String, BackendSession> sessions;

	private ISyncStorage storage;

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Continuation c = ContinuationSupport.getContinuation(request, request);
		logger.info("c.isPending() => " + c.isPending() + " c.isResumed() => "
				+ c.isResumed() + " method: " + request.getMethod() + " uri: "
				+ request.getRequestURI() + " " + request.getQueryString());
		if (c.isResumed()) {
			PingHandler ph = (PingHandler) handlers.get("Ping");
			ph.sendResponse((BackendSession) c.getObject(), new Responder(
					response));
			return;
		}

		String userId = null;
		String password = null;
		boolean valid = false;

		String m = request.getMethod();
		if ("OPTIONS".equals(m)) {
			sendServerInfos(response);
			return;
		}

		String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					String credentials = st.nextToken();
					String userPass = new String(Base64.decode(credentials
							.toCharArray()));
					int p = userPass.indexOf(":");
					if (p != -1) {
						userId = userPass.substring(0, p);
						password = userPass.substring(p + 1);
						String loginAtDomain = getLoginAtDomain(userId);
						valid = validatePassword(loginAtDomain, password);
						valid = valid
								&& storage
										.initDevice(loginAtDomain, p(request,
												"DeviceId"),
												extractDeviceType(request));
					}
				}
			}
		}

		if (!valid) {
			String uri = request.getMethod() + " " + request.getRequestURI()
					+ " " + request.getQueryString();
			logger.warn("invalid auth, sending http 401 (uri: " + uri + ")");
			String s = "Basic realm=\"OBMPushService\"";
			response.setHeader("WWW-Authenticate", s);
			response.setStatus(401);
		} else {
			processActiveSyncMethod(c, userId, password, request, response);
		}
	}

	private String extractDeviceType(HttpServletRequest request) {
		String ret = p(request, "DeviceType");
		if (ret.startsWith("IMEI")) {
			ret = request.getHeader("User-Agent");
		}
		return ret;
	}

	private String p(HttpServletRequest r, String name) {
		return r.getParameter(name);
	}

	private void processActiveSyncMethod(Continuation continuation,
			String userID, String password, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		BackendSession bs = getSession(userID, password, request);
		logger.info("activeSyncMethod: " + bs.getCommand());
		String proto = request.getHeader("MS-ASProtocolVersion");
		bs.setProtocolVersion(Double.parseDouble(proto));
		logger.info("Client supports protocol " + proto);

		if (bs.getCommand() == null) {
			logger.warn("POST received without explicit command, aborting");
			return;
		}

		InputStream in = request.getInputStream();
		byte[] input = FileUtils.streamBytes(in, true);
		Document doc = null;
		try {
			doc = WBXMLTools.toXml(input);
		} catch (IOException e) {
			logger.error("Error parsing wbxml data.", e);
			return;
		}

		logger.info("from pda:");
		try {
			DOMUtils.logDom(doc);
		} catch (TransformerException e) {
		}

		IRequestHandler rh = getHandler(bs);
		if (rh != null) {
			sendServerInfos(response);
			rh.process(continuation, bs, doc, new Responder(response));
		} else {
			logger.warn("no handler for command " + bs.getCommand());
		}
	}

	private BackendSession getSession(String userID, String password,
			HttpServletRequest r) {
		String uid = getLoginAtDomain(userID);

		BackendSession bs = null;
		if (sessions.containsKey(uid)) {
			bs = sessions.get(uid);
			bs.setCommand(p(r, "Cmd"));
		} else {
			bs = new BackendSession(uid, password, p(r, "DeviceId"),
					extractDeviceType(r), p(r, "Cmd"));
			new HintsLoader().addHints(r, bs);
		}
		bs.setRequest(r);
		return bs;
	}

	private String getLoginAtDomain(String userID) {
		String uid = userID;
		int idx = uid.indexOf("\\");
		if (idx > 0) {
			if (!uid.contains("@")) {
				String domain = uid.substring(0, idx);
				logger.info("uid: " + uid + " domain: " + domain);
				uid = uid.substring(idx + 1) + "@" + domain;
			} else {
				uid = uid.substring(idx + 1);
			}
		}
		return uid;
	}

	private void sendServerInfos(HttpServletResponse response) {
		response.setHeader("MS-Server-ActiveSync", "8.1");
		response.setHeader("X-MS-RP", "1.0,2.0,2.1,2.5,12.0,12.1");
		response
				.setHeader("MS-ASProtocolVersions", "1.0,2.0,2.1,2.5,12.0,12.1");
		response
				.setHeader(
						"MS-ASProtocolCommands",
						"Sync,SendMail,SmartForward,SmartReply,GetAttachment,GetHierarchy,CreateCollection,DeleteCollection,MoveCollection,FolderSync,FolderCreate,FolderDelete,FolderUpdate,MoveItems,GetItemEstimate,MeetingResponse,ResolveRecipipents,ValidateCert,Provision,Search,Ping");
	}

	private IRequestHandler getHandler(BackendSession p) {
		return handlers.get(p.getCommand());
	}

	private boolean validatePassword(String userID, String password) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void init() throws ServletException {
		super.init();

		PushConfiguration pc = new PushConfiguration();

		IBackend backend = loadBackend(pc);

		sessions = new HashMap<String, BackendSession>();

		handlers = new HashMap<String, IRequestHandler>();
		handlers.put("FolderSync", new FolderSyncHandler(backend));
		handlers.put("Sync", new SyncHandler(backend));
		handlers.put("GetItemEstimate", new GetItemEstimateHandler(backend));
		handlers.put("Provision", new ProvisionHandler(backend));
		handlers.put("Ping", new PingHandler(backend));
		handlers.put("Settings", new SettingsHandler(backend));

		System.out.println("ActiveSync servlet initialised.");
	}

	private IBackend loadBackend(PushConfiguration pc) {
		RunnableExtensionLoader<IStorageFactory> sto = new RunnableExtensionLoader<IStorageFactory>();
		List<IStorageFactory> storages = sto.loadExtensions("org.obm.push",
				"storage", "storage", "implementation");
		if (storages.size() > 0) {
			IStorageFactory stoFactoryImpl = storages.get(0);
			storage = stoFactoryImpl.createStorage();

		} else {
			logger.error("No storage implementation found");
			return null;
		}

		RunnableExtensionLoader<IBackendFactory> rel = new RunnableExtensionLoader<IBackendFactory>();
		List<IBackendFactory> backs = rel.loadExtensions("org.obm.push",
				"backend", "backend", "implementation");
		if (backs.size() > 0) {
			IBackendFactory bf = backs.get(0);
			return bf.loadBackend(storage, pc);
		} else {
			logger.error("No push backend found.");
			return null;
		}

	}

}
