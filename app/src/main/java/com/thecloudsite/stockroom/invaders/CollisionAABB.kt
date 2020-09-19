package com.thecloudsite.stockroom.invaders

class CollisionAABB(val player: GameObject, val protagonists: ArrayList<GameObject>, val neutral: ArrayList<GameObject>, val antagonist: ArrayList<GameObject>) :
    ICollisionHandler {
    override fun update(gameStatus: GameStatus) {

        // Check all protagonists against antagonists
        val protIt = protagonists.iterator()
        while(protIt.hasNext()) {
            val prot = protIt.next()
            val antIt = antagonist.iterator()
            while(antIt.hasNext()) {
                val ant = antIt.next()
                if(collides(prot, ant)) {
                    protIt.remove()
                    antIt.remove()
                    gameStatus.score += 10
                    break
                }
            }
        }

        // Check player against all antagonists
        val antIt = antagonist.iterator()
        while(antIt.hasNext()) {
            val ant = antIt.next()
            if(collides(player, ant)) {
                antIt.remove()
                gameStatus.lives -= 1
                continue
            }
        }

        // Check all protagonists against neutrals
        val protIt2 = protagonists.iterator()
        while(protIt2.hasNext()) {
            val prot = protIt2.next()
            val neutralIt = neutral.iterator()
            while(neutralIt.hasNext()) {
                val neut = neutralIt.next()
                if(collides(prot, neut)) {
                    protIt2.remove()
                    neutralIt.remove()
                    break
                }
            }
        }

        // Check all antagonists against neutrals
        val antIt2 = antagonist.iterator()
        while(antIt2.hasNext()) {
            val ant = antIt2.next()
            val neutralIt = neutral.iterator()
            while(neutralIt.hasNext()) {
                val neut = neutralIt.next()
                if(collides(ant, neut)) {
                    antIt2.remove()
                    neutralIt.remove()
                    break
                }
            }
        }
    }

    private fun collides(a: GameObject, b: GameObject): Boolean {
        if (a.position.left < b.position.right &&
            a.position.right > b.position.left &&
            a.position.top < b.position.bottom &&
            a.position.bottom > b.position.top)
            return true

        return false
    }
}