package org.obm.push.searchsource.ldap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.push.backend.BackendSession;
import org.obm.push.search.ISearchSource;
import org.obm.push.search.SearchResult;
import org.obm.push.search.StoreName;
import org.obm.push.utils.LdapUtils;

public class BookSource implements ISearchSource {

	private Configuration conf;
	private Log logger;

	public BookSource() {
		this.logger = LogFactory.getLog(getClass());
		conf = new Configuration();
	}

	private String uniqueAttribute(String string, Map<String, List<String>> m) {
		List<String> cnl = m.get(string);
		if (cnl == null || cnl.size() == 0) {
			return "";
		} else {
			return cnl.get(0);
		}
	}

	@Override
	public StoreName getStoreName() {
		return StoreName.GAL;
	}

	@Override
	public List<SearchResult> search(BackendSession bs, String query,
			Integer limit) {
		List<SearchResult> ret = new LinkedList<SearchResult>();
		if (conf.isValid()) {
			DirContext ctx = null;
			String domain = "";
			int idx = bs.getLoginAtDomain().indexOf("@");
			if (idx > 0) {
				domain = bs.getLoginAtDomain().substring(idx + 1);
			}
			try {
				ctx = conf.getConnection();
				LdapUtils u = new LdapUtils(ctx, conf.getBaseDn().replaceAll("%d",
						domain));
				List<Map<String, List<String>>> l = u.getAttributes(conf
						.getFilter(), query, new String[] { "cn", "sn",
						"givenName", "mail", "telephoneNumber", "mobile" });
				l = l.subList(0, Math.min(limit, l.size()));
				for (Map<String, List<String>> m : l) {
					String sn = uniqueAttribute("sn", m);
					String givenName = uniqueAttribute("givenName", m);
					String cn = uniqueAttribute("cn", m);
					List<String> phones = m.get("telephoneNumber");
					if (sn.length() == 0 || givenName.length() == 0) {
						sn = cn;
						givenName = "";
					}
					SearchResult sr = new SearchResult();
					sr.setDisplayName(givenName + " " + sn);
					sr.setLastName(sn);
					sr.setFirstName(givenName);
					if (phones != null) {
						if (phones.size() > 0) {
							sr.setPhone(phones.get(0));
						}
						if (phones.size() > 1) {
							sr.setHomePhone(phones.get(1));
						}
					}
					sr.setMobilePhone(uniqueAttribute("mobile", m));
					List<String> mails = m.get("mail");
					if (mails !=null && mails.iterator().hasNext()) {
						sr.setEmailAddress(mails.iterator().next());
					}
					ret.add(sr);
				}
			} catch (NamingException e) {
				logger.error("findAll error", e);
			} finally {
				conf.cleanup(ctx);
			}
		}
		return ret;
	}
}
