package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ChatController {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    @Autowired
    public ChatController (ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }
    @GetMapping("/")
    public ResponseDTO getLLMResponse(@RequestParam (value = "prompt", defaultValue = "Give me a fun small passage contrasting US and Indian economy") String prompt) {
        BeanOutputConverter<ResponseDTO> beanOutputConverter = new BeanOutputConverter<>(ResponseDTO.class);
        String format = beanOutputConverter.getFormat();

        ChatResponse chatResponse = chatClient.prompt()
                .advisors(
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()),
                        new MessageChatMemoryAdvisor(new InMemoryChatMemory())
                )
                .system(systemSpec -> systemSpec
                        .text("Output is needed as JSON with the format {format}")
                        .param("format", format))
                .user(userSpec -> userSpec
                        .text(prompt))
                .call().chatResponse();

        System.out.println("chat response is: " + chatResponse);

        ChatResponseMetadata chatResponseMetadata = chatResponse.getMetadata();
        Usage usage = chatResponseMetadata.getUsage();
        long promptTokenCount = usage.getPromptTokens();
        long genTokenCount = usage.getGenerationTokens();
        long totalTokenCount = usage.getTotalTokens();

        LOG.info("Token usage: prompt={}, generation={}, total={}", promptTokenCount, genTokenCount, totalTokenCount);
        return beanOutputConverter.convert(chatResponse.getResult().getOutput().getContent());
    }

    @PostMapping("/upload")
    public String documentToVectors(@RequestParam("file") MultipartFile file, @RequestParam("companyCode") String companyCode) {
        Resource resource = file.getResource();

        // ETL
        List<Document> documentList = new TikaDocumentReader(resource).get();
        for (Document document : documentList) {
            document.getMetadata().put("companyCode", companyCode);
        }
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> transFormedDocuments = tokenTextSplitter.split(documentList);
        vectorStore.accept(transFormedDocuments);
        return "Its done!";
    }

}
