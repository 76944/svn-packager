package com.svnpackager.service;

import com.svnpackager.model.CommitRecord;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.*;

public class SvnService {

    static {
        SVNRepositoryFactoryImpl.setup();
    }

    public List<CommitRecord> getLog(String url, String username, String password,
                                      String svnPath, Date startDate, Date endDate) throws SVNException {
        SVNRepository repository = createRepository(url, username, password);
        List<CommitRecord> commits = new ArrayList<>();

        try {
            Date adjustedStartDate = null;
            if (startDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                adjustedStartDate = cal.getTime();
            }

            Date adjustedEndDate = null;
            if (endDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                adjustedEndDate = cal.getTime();
            }

            long startRevision = (adjustedStartDate != null)
                    ? repository.getDatedRevision(adjustedStartDate) : 0;
            long endRevision = (adjustedEndDate != null)
                    ? repository.getDatedRevision(adjustedEndDate) : repository.getLatestRevision();

            if (startRevision > endRevision) {
                long tmp = startRevision;
                startRevision = endRevision;
                endRevision = tmp;
            }

            String logPath = (svnPath != null && !svnPath.trim().isEmpty()) ? svnPath.trim() : "";
            Collection logEntries = repository.log(
                    new String[]{logPath},
                    null,
                    startRevision,
                    endRevision,
                    true,
                    true
            );

            for (Object obj : logEntries) {
                SVNLogEntry logEntry = (SVNLogEntry) obj;
                Date entryDate = logEntry.getDate();

                if (adjustedStartDate != null && entryDate != null && entryDate.before(adjustedStartDate)) {
                    continue;
                }
                if (adjustedEndDate != null && entryDate != null && entryDate.after(adjustedEndDate)) {
                    continue;
                }

                CommitRecord record = new CommitRecord(
                        logEntry.getRevision(),
                        logEntry.getAuthor(),
                        logEntry.getDate(),
                        logEntry.getMessage()
                );

                Set<String> changedPaths = logEntry.getChangedPaths().keySet();
                record.setChangedPaths(new ArrayList<>(changedPaths));

                commits.add(record);
            }
        } finally {
            repository.closeSession();
        }

        commits.sort((a, b) -> Long.compare(b.getRevision(), a.getRevision()));
        return commits;
    }

    public List<String> getChangedFiles(String url, String username, String password,
                                         long revision) throws SVNException {
        SVNRepository repository = createRepository(url, username, password);
        try {
            Collection logEntries = repository.log(
                    new String[]{""},
                    null,
                    revision,
                    revision,
                    true,
                    true
            );

            for (Object obj : logEntries) {
                SVNLogEntry logEntry = (SVNLogEntry) obj;
                return new ArrayList<>(logEntry.getChangedPaths().keySet());
            }
        } finally {
            repository.closeSession();
        }

        return Collections.emptyList();
    }

    public void checkoutFiles(String url, String username, String password,
                               long revision, File targetDir) throws SVNException {
        SVNRepository repository = createRepository(url, username, password);
        try {
            SVNUpdateClient updateClient = new SVNUpdateClient(
                    repository.getAuthenticationManager(),
                    SVNWCUtil.createDefaultOptions(true));
            updateClient.doCheckout(
                    SVNURL.parseURIDecoded(url),
                    targetDir,
                    SVNRevision.HEAD,
                    SVNRevision.create(revision),
                    true
            );
        } finally {
            repository.closeSession();
        }
    }

    public boolean testConnection(String url, String username, String password) {
        SVNRepository repository = null;
        try {
            repository = createRepository(url, username, password);
            repository.getLatestRevision();
            return true;
        } catch (SVNException e) {
            return false;
        } finally {
            if (repository != null) {
                repository.closeSession();
            }
        }
    }

    private SVNRepository createRepository(String url, String username, String password) throws SVNException {
        SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(
                username, password != null ? password.toCharArray() : new char[0]);
        repository.setAuthenticationManager(authManager);
        return repository;
    }
}
