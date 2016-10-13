/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.dp.invocable.ftp.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.dp.Steps;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.FTPConnection;
import rapture.ftp.common.FTPRequest;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.SFTPConnection;
import rapture.kernel.Kernel;

public class CheckFileExistsStep extends AbstractInvocable {
    private static final Logger log = Logger.getLogger(CopyFileStep.class);

    DecisionApi decision;
    public CheckFileExistsStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    String wasNotWas(Boolean flag) {
        return (flag) ? " was " : " was not ";
    }

    /**
     * FTP_CONFIGURATION is optional. If not set the arguments are assumed to be local EXIST_FILENAMES is a map of file names to Booleans, indicating whether
     * the file is expected or not
     */
    @Override
    public String invoke(CallingContext ctx) {
        String workUri = getWorkerURI();
        try {
            decision.setContextLiteral(ctx, workUri, "STEPNAME", getStepName());

            String configUri = StringUtils.stripToNull(decision.getContextValue(ctx, workUri, "FTP_CONFIGURATION"));
            String filename = StringUtils.stripToNull(decision.getContextValue(ctx, workUri, "EXIST_FILENAMES"));
            if (filename == null) {
                decision.setContextLiteral(ctx, workUri, getStepName(), "No files to check");
                decision.setContextLiteral(ctx, workUri, getStepName() + "Error", "");
                return getNextTransition();
            }

            Map<String, Object> files = JacksonUtil.objectFromJson(filename, Map.class);

            FTPConnection connection = new SFTPConnection(configUri).setContext(ctx);
            String retval = getNextTransition();
            List<FTPRequest> requests = new ArrayList<>();
            int existsCount = 0;
            int failCount = 0;
            StringBuilder error = new StringBuilder();
            for (Entry<String, Object> e : files.entrySet()) {
                FTPRequest request = new FTPRequest(Action.EXISTS).setRemoteName(e.getKey());
                boolean exists = connection.doAction(request);
                if (!exists == ((Boolean) e.getValue())) {
                    retval = getFailTransition();
                    error.append(e.getKey()).append(wasNotWas(exists)).append("found but").append(wasNotWas((Boolean) e.getValue())).append("expected ");
                    failCount++;
                }
                requests.add(request);
            }
            decision.setContextLiteral(ctx, workUri, getStepName(), "Located " + existsCount + " of " + files.size() + " files");
            String errMsg = error.toString();
            decision.setContextLiteral(ctx, workUri, getStepName() + "Error", errMsg);
            decision.writeWorkflowAuditEntry(ctx, workUri, errMsg, failCount > 0);
            return retval;
        } catch (Exception e) {
            decision.setContextLiteral(ctx, workUri, getStepName(), "Unable to determine if files exist : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, workUri, getStepName() + "Error", ExceptionToString.summary(e));
            decision.writeWorkflowAuditEntry(ctx, workUri, ExceptionToString.summary(e), true);
            return getErrorTransition();
        }
    }

    public static String getFailTransition() {
        return Steps.WAIT;
    }

}
