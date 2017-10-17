package io.redlink.smarti.services;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.repositories.AnalysisRepository;

@Service
public class AnalysisService {

    private final AnalysisRepository analysisRepo;
    
    public AnalysisService(AnalysisRepository analysisRepo) {
        this.analysisRepo = analysisRepo;
    }

    
    public Analysis getAnalysis(Conversation con){
        return analysisRepo.findByConversationAndDate(con.getId(), con.getLastModified());
    }
    
    public Analysis getAnalysis(ObjectId id){
        return analysisRepo.findOne(id);
    }
    
    public List<Analysis> getAnalysisForConversation(ObjectId conversationId){
        return analysisRepo.findByConversation(conversationId);
    }
}
