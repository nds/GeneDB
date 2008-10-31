package org.genedb.web.mvc.controller;

import org.genedb.db.dao.SequenceDao;
import org.genedb.querying.history.HistoryManager;
import org.genedb.querying.history.HistoryType;

import org.gmod.schema.mapped.Feature;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.google.common.collect.Lists;

/**
 * Controller for uploading a list of ids, either from a file or a textbox.
 * After validating them it stores them in the user's history
 */
@Controller
@RequestMapping("/IdList")
public class IdListController {

    private HistoryManagerFactory hmFactory;

    private SequenceDao sequenceDao;

    @RequestMapping(method=RequestMethod.GET)
    public String prepareForm() {
        return "analysis/idList";
    }

    @RequestMapping(method=RequestMethod.POST)
    public String processImageUpload(
            HttpSession session,
            @RequestParam("idList") String idList,
            @RequestParam("oldIds") boolean useOldIds,
            @RequestParam("historyItemName") String historyItemName,
            @RequestParam("ids") MultipartFile mf) {

        List<String> ids = Lists.newArrayList();

        if (!mf.isEmpty()) {
            try {
                addIds(ids, new String(mf.getBytes()));
            }
            catch (IOException exp) {
                exp.printStackTrace();
                return "redirect:internalError";
            }
        }

        if (StringUtils.hasText(idList)) {
            addIds(ids, idList);
        }

        List<String> okIds = Lists.newArrayList();
        List<String> oldIds = Lists.newArrayList();
        List<String> ambiguousIds = Lists.newArrayList();
        List<String> badIds = Lists.newArrayList();
        validateIds(ids, okIds, oldIds, ambiguousIds, badIds);

        if (badIds.size() > 0) {
            // Don't recognize all the ids

            return "redirect:warnUser";
        }

        HistoryManager hm = hmFactory.getHistoryManager(session);

        if (useOldIds) {
            okIds.addAll(oldIds);
        }

        String historyName = "Uploaded";
        if (StringUtils.hasText(historyItemName)) {
            historyName = historyItemName;
        }
        hm.addHistoryItem(historyName, HistoryType.MANUAL, okIds);

        return "redirect:/History";
    }


    private void validateIds(List<String> ids, List<String> okIds,
            List<String> oldIds, List<String> ambiguousIds, List<String> badIds) {

        for (String id : ids) {
            if (validatePrimaryId(id)) {
                okIds.add(id);
                continue;
            }
            if (validateSecondaryId(id, oldIds, ambiguousIds)) {
                continue;
            }
            badIds.add(id);
        }
    }


    private boolean validateSecondaryId(String id, List<String> oldIds, List<String> ambiguousIds) {
        List<Feature> lf = sequenceDao.getFeaturesByAnyCurrentName(id); // TODO Is logic right
        if (lf.size() == 0) {
            return false;
        }
        if (lf.size() > 1) {
            ambiguousIds.add(id);
            return true;
        }
        oldIds.add(id);
        return true;
    }


    private boolean validatePrimaryId(String id) {
        return sequenceDao.getFeatureByUniqueName(id, Feature.class) != null;
    }


    private void addIds(List<String> ids, String idList) {
        String[] split= idList.split("\\n");
        ids.addAll(Arrays.asList(split));
    }

}