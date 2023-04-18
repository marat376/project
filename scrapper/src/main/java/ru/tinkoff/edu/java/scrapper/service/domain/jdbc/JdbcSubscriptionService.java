package ru.tinkoff.edu.java.scrapper.service.domain.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import ru.tinkoff.edu.java.scrapper.model.entity.ChatEntity;
import ru.tinkoff.edu.java.scrapper.model.entity.LinkEntity;
import ru.tinkoff.edu.java.scrapper.repository.jdbc.JdbcChatRepository;
import ru.tinkoff.edu.java.scrapper.repository.jdbc.JdbcLinkRepository;
import ru.tinkoff.edu.java.scrapper.repository.jdbc.JdbcSubscriptionRepository;
import ru.tinkoff.edu.java.scrapper.service.domain.api.SubscriptionService;

import java.net.URI;
import java.util.List;

@Slf4j
//@Service
@RequiredArgsConstructor
public class JdbcSubscriptionService implements SubscriptionService {
    private final JdbcSubscriptionRepository subscriptionRepository;
    private final JdbcLinkRepository linkRepository;
    private final JdbcChatRepository chatRepository;

    @Override
    @Transactional
    public LinkEntity subscribe(Long chatId, URI url) {
        try {
            LinkEntity linkEntity = linkRepository.find(url.toString());
            subscriptionRepository.add(chatId, linkEntity.getId());
            return linkEntity;
        } catch (EmptyResultDataAccessException ignored) {
            Long linkId = linkRepository.add(url.toString());
            try {
                subscriptionRepository.add(chatId, linkId);
            } catch (DuplicateKeyException e) {
                log.error(e.getMessage());
                throw new IllegalArgumentException("Subscription already exist", e);
            }
            return linkRepository.findById(linkId);
        }
    }

    @Override
    @Transactional
    public LinkEntity unsubscribe(Long chatId, URI url) {
        try {
            LinkEntity linkEntity = linkRepository.find(url.toString());
            subscriptionRepository.remove(chatId, linkEntity.getId());
            Integer subscriptions = subscriptionRepository.countSubscriptions(linkEntity.getId());
            if (subscriptions == 0) {
                linkRepository.removeById(linkEntity.getId());
            }
            return linkEntity;
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException("Link not found", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkEntity> getChatSubscriptions(Long chatId) {
        return linkRepository.findWithSubscriber(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatEntity> getLinkSubscribers(Long linkId) {
        return chatRepository.findAllSubscribers(linkId);
    }
}