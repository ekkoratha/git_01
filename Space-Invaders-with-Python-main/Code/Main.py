# Tutorial: https://youtu.be/o-6pADy5Mdg

import pygame, sys
from Player import Player
import Obstacle
from Alien import Alien, Extra
from Laser import Laser
from random import choice, randint
import os


# File Importing (Changes Directory to Where the File is Saved)
os.chdir(os.path.dirname(os.path.abspath(__file__)))


class Game:
    def __init__(self): #initiate method ..for all sprites
        # Game Window UI and Icon
        pygame .display.set_caption("Space Invaders")
        pygame_icon = pygame.image.load("../Graphics/Red.png")
        pygame.display.set_icon(pygame_icon)

        # Player Setup
        player_sprite = Player((screen_width / 2, screen_height), screen_width, 5) # need to be bottom of the screen and in the middle..
        self.player = pygame.sprite.GroupSingle(player_sprite)

        # Health and Score Setup
        self.lives = 3
        self.live_surf = pygame.image.load("../Graphics/Player2.png").convert_alpha()
        self.live_x_start_pos = screen_width - (self.live_surf.get_size()[0] * 4 + 20)
        self.score = 0
        self.font = pygame.font.Font("../Font/Pixeled.ttf", 20)

        # Obstacle Setup
        self.shape = Obstacle.shape
        self.block_size = 6
        self.blocks = pygame.sprite.Group()
        self.obstacle_amount = 4
        self.obstacle_x_positions = [num * (screen_width / self.obstacle_amount) for num in range(self.obstacle_amount)]
        # create multi block
        self.create_multiple_obstacles(*self.obstacle_x_positions, x_start = screen_width / 15, y_start = 480)

        # ------------
        # Alien Setup
        self.aliens = pygame.sprite.Group()
        self.alien_lasers = pygame.sprite.Group()
        self.alien_setup(rows = 6, cols = 8)
        self.alien_direction = 1  

        # -------------------
        # Extra Alien Setup
        self.extra = pygame.sprite.GroupSingle() 
        self.extra_spawn_time = randint(400, 800)

        # Audio
        self.audio_muted=1
        if self.audio_muted == 0:
            music = pygame.mixer.Sound("../Audio/Music.wav")
            music.set_volume(0.5)
            music.play(loops = -1)
        self.laser_sound = pygame.mixer.Sound('../Audio/Laser.wav')
        self.laser_sound.set_volume(0.5)
        self.explosion_sound = pygame.mixer.Sound('../Audio/Explosion.wav')
        self.explosion_sound.set_volume(0.5)
#----------------------------
# CREATE OBSTACLE
    def create_obstacle(self, x_start, y_start, offset_x):
        for row_index, row in enumerate(self.shape):
            for col_index, col in enumerate(row):
                if col == "x":
                    x = x_start + col_index * self.block_size + offset_x
                    y = y_start + row_index * self.block_size
                    block = Obstacle.Block(self.block_size, (241, 79, 80), x, y) # Obstacle block (Where we see a 'x' make a Block) with colorRGB(241, 79, 80) and size=6
                    self.blocks.add(block)
#----------------------------
# CREATE MULTI- OBSTACLE
    def create_multiple_obstacles(self, *offset, x_start, y_start):
        for offset_x in offset:
            self.create_obstacle(x_start, y_start, offset_x)
#----------------------------
# ALIEN SETUP
    def alien_setup(self, rows, cols, x_distance = 60, y_distance = 48, x_offset = 70, y_offset = 100):
        for row_index, row in enumerate(range(rows)):
            for col_index, col in enumerate(range(cols)):
                x = col_index * x_distance + x_offset
                y = row_index * y_distance + y_offset
                
                # create the alien sprite with color and position 
                if row_index == 0: alien_sprite = Alien("Yellow2", x, y)
                elif 1 <= row_index <= 2: alien_sprite = Alien("Green2", x, y)
                else: alien_sprite = Alien("Red2", x, y)
                self.aliens.add(alien_sprite)

    def alien_position_checker(self):
        all_aliens = self.aliens.sprites()
        for alien in all_aliens:
            if alien.rect.right >= screen_width:
                self.alien_direction = -1 # all Aliens hit the right side so now set to -1
                self.alien_move_down(1)
            elif alien.rect.left <= 0:
                self.alien_direction = 1  # all Aliens  hit the left side so now set to -1
                self.alien_move_down(1)

    def alien_move_down(self, distance):
        if self.aliens:
            for alien in self.aliens.sprites():
                alien.rect.y += distance

# ---------
# Alient (random) shooting the SPACE SHIP
    def alien_shoot(self):
        if self.aliens.sprites():
            random_alien = choice(self.aliens.sprites())
            laser_sprite = Laser(random_alien.rect.center, 6, screen_height)
            self.alien_lasers.add(laser_sprite)
            self.laser_sound.play()
# ---------
# EXTRA Alient timer spawned using (random)this has a value of 500
    def extra_alien_timer(self):
        self.extra_spawn_time -= 1
        if self.extra_spawn_time <= 0:
            self.extra.add(Extra(choice(["right", "left"]), screen_width, ))
            self.extra_spawn_time = randint(400, 800)

    def collision_checks(self):

        #  ***********
        # Player Lasers = 1.1 obstacles + 1.2 aliens + 1.3 extra alien
        # ***********
        if self.player.sprite.lasers:
            for laser in self.player.sprite.lasers:
                # 1.2 Obstacle Collisions
                if pygame.sprite.spritecollide(laser, self.blocks, True):
                    laser.kill()

                # 1.2 Alien Collisions
                aliens_hit = pygame.sprite.spritecollide(laser, self.aliens, True) #self.aliens (SPRITE COLLIDE)
                if aliens_hit:
                    for alien in aliens_hit:
                        self.score += alien.value
                    screen.fill((255, 255, 255))
                    laser.kill()
                    self.explosion_sound.play()
 
                # 1.3 Extra Collisions
                if pygame.sprite.spritecollide(laser, self.extra, True):  #self.extra (SPRITE COLLIDE)
                    screen.fill((255, 255, 255))
                    self.score += 500
                    laser.kill()
                    self.explosion_sound.play()
        
        # *******************
        # Alien Lasers  --> obstacles + players
        # ******************
        if self.alien_lasers:
            for laser in self.alien_lasers:
                # Obstacle Collisions
                if pygame.sprite.spritecollide(laser, self.blocks, True):
                    laser.kill()

                # Player Collisions
                if pygame.sprite.spritecollide(laser, self.player, False):
                    laser.kill()
                    screen.fill((122, 0, 0))
                    self.lives -= 1
                    
        # Aliens
        if self.aliens:
            for alien in self.aliens:
                # Obstacle Collisions
                pygame.sprite.spritecollide(alien, self.blocks, True)

                # Player Collisions
                if pygame.sprite.spritecollide(alien, self.player, True):
                    pygame.quit()
                    sys.exit()

    def display_lives(self):
        for live in range(self.lives - 1):
            x = self.live_x_start_pos + (live * (self.live_surf.get_size()[0] + 10))
            screen.blit(self.live_surf, (x, 8))

        if self.lives <= 0:
            pygame.quit()
            sys.exit()

    def display_score(self):
        score_surf = self.font.render(f"Score: {self.score}", False, "White")
        score_rect = score_surf.get_rect(topleft = (10, -10))
        screen.blit(score_surf, score_rect)

    def victory_message(self):
        if not self.aliens.sprites():
            victory_surf = self.font.render("You Won!", False, "White")
            victory_rect = victory_surf.get_rect(center = (screen_width / 2, screen_height/ 2))
            screen.blit(victory_surf, victory_rect)
 
    def run(self): # update all sprite grps and run all sprite groups
        # Draw and Update All Sprite Groups
        self.player.update()
        self.alien_lasers.update()
        self.extra.update()

        self.aliens.update(self.alien_direction)
        self.alien_position_checker()
        self.extra_alien_timer()
        self.collision_checks() # 1.1 1.2 1.3 

        self.player.sprite.lasers.draw(screen)
        self.player.draw(screen)
        self.blocks.draw(screen)
        self.aliens.draw(screen)
        self.alien_lasers.draw(screen)
        self.extra.draw(screen)
        self.display_lives()
        self.display_score()
        self.victory_message()


class CRT:
    def __init__(self):
        self.tv = pygame.image.load("../Graphics/TV.png").convert_alpha()
        self.tv = pygame.transform.scale(self.tv, (screen_width, screen_height))

    def create_crt_lines(self):
        line_height = 3
        line_amount = int(screen_height / line_height)
        for line in range(line_amount):
            y_pos = line * line_height
            pygame.draw.line(self.tv, "Black", (0, y_pos), (screen_width, y_pos), 1)

    def draw(self):
        self.tv.set_alpha(randint(75, 90))
        self.create_crt_lines()
        screen.blit(self.tv, (0, 0))


if __name__ == "__main__":
    pygame.init()
    screen_width = 600
    screen_height = 600
    screen = pygame.display.set_mode((screen_width, screen_height))
    clock = pygame.time.Clock()
    # lets create instance of game
    game = Game()
    crt = CRT()

    ALIENLASER = pygame.USEREVENT + 1
    pygame.time.set_timer(ALIENLASER, 800)

    while True:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit()
                sys.exit()
            if event.type == ALIENLASER:
                game.alien_shoot()

        screen.fill((30, 30, 30))
        game.run() # game run will do actual run
        crt.draw()

        pygame.display.flip()
        clock.tick(100) #60)
