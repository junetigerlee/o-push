# Introduction #

This page explains how to run the binary releases available from this site.

# Requirements #

Before starting the o-push binary, you need to have the following components up & running :

  * OBM-Sync server >= 2.2.7
  * /etc/obm/obm\_conf.ini from a working obm 2.2.x PostgreSQL installation
  * correct firewall setup. o-push will connect to :
    * obm-locator to locate where you obm-sync server is.
    * Your obm-sync host via http://obm-sync-host:8080/obm-sync/services
  * A working apache server that will be your entry point to o-push and will reverse proxy all your queries to it.

# Apache setup #

In the virtual host doing your reverse proxy stuff (probably the one declared as your "external url" for obm). Add the following lines :

```
    ProxyPass /Microsoft-Server-ActiveSync http://localhost:8082/Microsoft-Server-ActiveSync
    ProxyPassReverse /Microsoft-Server-ActiveSync http://localhost:8082/Microsoft-Server-ActiveSync
```

_replace localhost by the fqdn of the host that will run o-push_

# Database setup #

  * Get the SQL scripts from o-push svn : http://code.google.com/p/o-push/source/browse/#svn/trunk/sql/2.2
  * run the following command :
```
./install_opush_db.sh <db> <db_user> <db_host>
```

Where db is your obm database name, db\_user the user used by obm interface to access its database and db\_host is the ip address of your obm PostgreSQL server.

For example :
```
./install_opush_db.sh obm obmuser 10.0.0.5
```

This will only create new tables in your obm database. It will not touch existing data.

# Startup #

When all the previous steps are ok, just un-tar the o-push binary & run

```
./o-push
```

This should show something like that :

```
Push server started...
2009-07-21 21:40:37,247 RunnableExtensionLoader INFO - StorageFactory loaded.
2009-07-21 21:40:37,248 RunnableExtensionLoader INFO - Loaded 1 implementors of org.obm.push.storage
2009-07-21 21:40:37,252 RunnableExtensionLoader INFO - BackendFactory loaded.
2009-07-21 21:40:37,252 RunnableExtensionLoader INFO - Loaded 1 implementors of org.obm.push.backend
2009-07-21 21:40:37,252 BackendFactory INFO - Loading OBM 2.2.x backend...
2009-07-21 21:40:37,256 OBMPoolActivator INFO - Starting OBM connection pool...
2009-07-21 21:40:37,258 OBMPoolActivator INFO - dbtype from obm_conf.ini is PGSQL
2009-07-21 21:40:37,262 RunnableExtensionLoader INFO - PgSQLConnectionFactory loaded.
2009-07-21 21:40:37,262 RunnableExtensionLoader INFO - Loaded 1 implementors of org.minig.obm.pool.jdbcconnectionfactory
2009-07-21 21:40:37,270 Pool INFO - obmpool-obm: Adding pooled object...
2009-07-21 21:40:37,303 Pool INFO - obmpool-obm: Pooled object added.
2009-07-21 21:40:37,303 Pool INFO - obmpool-obm: Adding pooled object...
2009-07-21 21:40:37,313 Pool INFO - obmpool-obm: Pooled object added.
2009-07-21 21:40:37,313 Pool INFO - obmpool-obm: Adding pooled object...
2009-07-21 21:40:37,323 Pool INFO - obmpool-obm: Pooled object added.
2009-07-21 21:40:37,365 MailBackend INFO - OBM Db connection is OK
2009-07-21 21:40:37,370 CalendarBackend INFO - OBM Db connection is OK
2009-07-21 21:40:37,373 ContactsBackend INFO - OBM Db connection is OK
ActiveSync servlet initialised.
```

The server runs queries on your obm database when it starts, to locate the obm-sync it will use.

# Mobile device setup #

## iPhone 3.0 ##

Just declare a new Exchange mail account.

  * Your domain is your obm domain
  * Your login is your obm login@domain
  * Your password is your obm password.

Authentification will be performed by obm-sync (remember we said in the requirements that a working obm-sync server was needed).

## Windows mobile ##

Screenshots with step by step configuration are available : http://code.google.com/p/o-push/source/browse/#svn/trunk/docs/wm_configuration