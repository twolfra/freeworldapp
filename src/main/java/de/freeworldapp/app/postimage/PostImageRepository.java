package de.freeworldapp.app.postimage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    List<PostImage> findByTargetTypeAndTargetIdOrderBySortOrder(PostImage.TargetType targetType, UUID targetId);

    void deleteByTargetTypeAndTargetId(PostImage.TargetType targetType, UUID targetId);
}
